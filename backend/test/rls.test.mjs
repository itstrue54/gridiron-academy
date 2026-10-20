// Row-level security, tested on real Postgres (PGlite). Each test acts as one person and
// checks what the database lets them see and do (SPEC 5.4, 7.1, 8.5).
import { test, before } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { PGlite } from '@electric-sql/pglite'

const db = new PGlite()
const here = new URL('.', import.meta.url)
const migrations = new URL('../supabase/migrations/', import.meta.url)

const RAY = '00000000-0000-0000-0000-00000000000a'   // parent of Leo, coaches the Gulls
const PAT = '00000000-0000-0000-0000-00000000000b'   // another parent, child Mia on the Gulls
const EVE = '00000000-0000-0000-0000-00000000000c'   // a stranger
const JO = '00000000-0000-0000-0000-00000000000d'    // a teen with their own account
let leo, mia, jo, gulls

/** Run SQL as a signed-in user (or a learner device with a learner_id claim). */
async function as(claims, sql, params = []) {
  await db.exec('reset role')
  await db.query(`select set_config('request.jwt.claims', $1, false)`, [JSON.stringify(claims)])
  await db.exec('set role authenticated')
  try { return await db.query(sql, params) } finally { await db.exec('reset role') }
}
const user = (id) => ({ sub: id, role: 'authenticated' })
const rows = async (claims, sql, params) => (await as(claims, sql, params)).rows
const fails = async (claims, sql, params, pattern) =>
  assert.rejects(as(claims, sql, params), pattern ?? /row-level security|permission denied|violates check|required|only the parent/)

before(async () => {
  await db.exec(readFileSync(new URL('supabase-stub.sql', here), 'utf8'))
  for (const f of readdirSync(migrations).sort()) await db.exec(readFileSync(new URL(f, migrations), 'utf8'))
  for (const id of [RAY, PAT, EVE, JO]) await db.query('insert into auth.users values ($1)', [id])

  // Everyone signs up and makes their own account row.
  for (const [id, name] of [[RAY, 'Coach Ray'], [PAT, 'Pat'], [EVE, 'Eve'], [JO, 'Jo']]) {
    await as(user(id), 'insert into account (id, display_name) values ($1, $2)', [id, name])
  }
  leo = (await rows(user(RAY), `insert into learner (nickname, age_band, track, managed_by) values ('Leo', '8-9', 'kid', $1) returning id`, [RAY]))[0].id
  mia = (await rows(user(PAT), `insert into learner (nickname, age_band, track, managed_by) values ('Mia', '10-12', 'kid', $1) returning id`, [PAT]))[0].id
  jo = (await rows(user(JO), `insert into learner (nickname, age_band, track, own_account) values ('Jo', '13-17', 'adult', $1) returning id`, [JO]))[0].id
  gulls = (await rows(user(RAY), `insert into team (owner_id, name, variant) values ($1, 'Harbor City Gulls', 'Youth') returning id`, [RAY]))[0].id
})

test('signed-out callers see nothing', async () => {
  await db.query(`select set_config('request.jwt.claims', '{}', false)`)
  await db.exec('set role anon')
  try {
    await assert.rejects(db.query('select * from learner'), /permission denied/)
  } finally { await db.exec('reset role') }
})

test('a parent sees their own child and no one else', async () => {
  assert.deepEqual((await rows(user(RAY), 'select nickname from learner order by 1')).map(r => r.nickname), ['Leo'])
  assert.deepEqual((await rows(user(PAT), 'select nickname from learner order by 1')).map(r => r.nickname), ['Mia'])
  assert.equal((await rows(user(EVE), 'select * from learner')).length, 0)
})

test('nobody can create a learner for someone else', async () => {
  await fails(user(EVE), `insert into learner (nickname, age_band, track, managed_by) values ('X', '8-9', 'kid', $1)`, [RAY])
})

test('creating a team makes the owner staff', async () => {
  const s = await rows(user(RAY), 'select role from staff where team_id = $1', [gulls])
  assert.deepEqual(s.map(r => r.role), ['owner'])
})

test('a coach invites, the parent approves, and only then the coach sees the child', async () => {
  await as(user(RAY), 'insert into team_member (team_id, learner_id) values ($1, $2)', [gulls, mia])
  // An invitation can't arrive pre-approved.
  await fails(user(RAY), `insert into team_member (team_id, learner_id, status) values ($1, $2, 'approved')`, [gulls, jo])
  // Pending: Ray can't see Mia.
  assert.equal((await rows(user(RAY), 'select * from learner where id = $1', [mia])).length, 0)
  // Staff can't approve, even with a direct update (status is not an updatable column for them).
  await fails(user(RAY), `update team_member set status = 'approved' where learner_id = $1`, [mia])
  await fails(user(RAY), 'select set_membership($1, $2, true)', [gulls, mia])
  // Mia is 10-12: no approval without consent.
  await fails(user(PAT), 'select set_membership($1, $2, true)', [gulls, mia], /consent/)
  await as(user(PAT), `insert into consent (learner_id, parent_id, method, version) values ($1, $2, 'test', 'v1')`, [mia, PAT])
  await as(user(PAT), 'select set_membership($1, $2, true)', [gulls, mia])
  assert.equal((await rows(user(RAY), 'select nickname from learner where id = $1', [mia]))[0].nickname, 'Mia')
  // Staff may set positions; the parent sees the team.
  await as(user(RAY), `update team_member set positions = '{LB}' where learner_id = $1`, [mia])
  assert.equal((await rows(user(PAT), 'select name from team'))[0].name, 'Harbor City Gulls')
  // A stranger still sees nothing.
  assert.equal((await rows(user(EVE), 'select * from team_member')).length, 0)
})

test('revoking a membership hides the child from the coach again', async () => {
  await as(user(PAT), 'select set_membership($1, $2, false)', [gulls, mia])
  assert.equal((await rows(user(RAY), 'select * from learner where id = $1', [mia])).length, 0)
  await as(user(PAT), 'select set_membership($1, $2, true)', [gulls, mia])
})

test('a teen with their own account approves their own membership', async () => {
  await as(user(RAY), 'insert into team_member (team_id, learner_id) values ($1, $2)', [gulls, jo])
  await as(user(JO), 'select set_membership($1, $2, true)', [gulls, jo])
  assert.equal((await rows(user(RAY), 'select * from learner where id = $1', [jo])).length, 1)
})

test('only a consented learner device writes its own events', async () => {
  const device = (id) => ({ sub: '', role: 'authenticated', learner_id: id })
  const ins = 'insert into event (learner_id, device_id, seq, payload) values ($1, $2, $3, $4)'
  // Leo has no consent yet: his device can't upload.
  await fails(device(leo), ins, [leo, 'tab1', 0, { stepId: 's1' }])
  await as(user(RAY), `insert into consent (learner_id, parent_id, method, version) values ($1, $2, 'test', 'v1')`, [leo, RAY])
  await as(device(leo), ins, [leo, 'tab1', 0, { stepId: 's1' }])
  // A device can't write for another learner, and a parent account can't forge events.
  await fails(device(leo), ins, [mia, 'tab1', 0, { stepId: 's1' }])
  await fails(user(RAY), ins, [leo, 'ray-phone', 0, { stepId: 's1' }])
  // A repeat upload is a primary-key conflict: idempotent.
  await assert.rejects(as(device(leo), ins, [leo, 'tab1', 0, { stepId: 's1' }]), /duplicate key/)
  // The parent reads it; the coach doesn't (Leo isn't on the team); the stranger doesn't.
  assert.equal((await rows(user(RAY), 'select * from event where learner_id = $1', [leo])).length, 1)
  assert.equal((await rows(user(EVE), 'select * from event')).length, 0)
  // Events are append-only: with no update or delete policy, both touch nothing.
  assert.equal((await rows(device(leo), 'update event set seq = 9 where learner_id = $1 returning *', [leo])).length, 0)
  assert.equal((await rows(device(leo), 'delete from event where learner_id = $1 returning *', [leo])).length, 0)
})

test('assignments: staff send to approved players only, and notes stay clean', async () => {
  const send = `insert into assignment (team_id, created_by, from_name, pack_ids, sent_day, note) values ($1, $2, 'Coach Ray', '{def-quarters}', 20000, $3) returning id`
  const a = (await rows(user(RAY), send, [gulls, RAY, 'Know these before Thursday.']))[0].id
  await as(user(RAY), 'insert into assignment_target (assignment_id, learner_id, lessons_at_send) values ($1, $2, 3)', [a, mia])
  // A learner Ray can't see (Pat's child before... or any stranger's) can't be targeted.
  const eveKid = (await rows(user(EVE), `insert into learner (nickname, age_band, track, managed_by) values ('Kid', '8-9', 'kid', $1) returning id`, [EVE]))[0].id
  await fails(user(RAY), 'insert into assignment_target (assignment_id, learner_id) values ($1, $2)', [a, eveKid])
  // Pat sees the assignment through Mia; Eve doesn't.
  assert.equal((await rows(user(PAT), 'select from_name from assignment'))[0].from_name, 'Coach Ray')
  assert.equal((await rows(user(EVE), 'select * from assignment')).length, 0)
  // Immutable once sent.
  assert.equal((await rows(user(RAY), `update assignment set note = 'changed' returning *`)).length, 0)
  // Links, emails and phone numbers never reach a child.
  for (const bad of ['see www.example.com', 'go to https://x.y', 'mail coach@team.org', 'text 555 123 4567', 'x'.repeat(201)]) {
    await fails(user(RAY), send, [gulls, RAY, bad], /violates check/)
  }
  // Someone who isn't staff can't send as the team.
  await fails(user(EVE), send, [gulls, EVE, ''])
})

test('send_assignment is all or nothing', async () => {
  const call = 'select send_assignment($1, $2, $3, $4, $5, $6, $7, $8) as id'
  const ok = (await rows(user(RAY), call, [gulls, 'Coach Ray', ['def-quarters'], 20007, 80, '', 20000,
    JSON.stringify([{ learner_id: mia, lessons_at_send: 2 }])]))[0].id
  assert.equal((await rows(user(RAY), 'select count(*)::int as n from assignment_target where assignment_id = $1', [ok]))[0].n, 1)
  const before = (await rows(user(RAY), 'select count(*)::int as n from assignment'))[0].n
  // One target Ray can't see: the whole send fails and leaves no assignment behind.
  const stranger = (await rows(user(EVE), `insert into learner (nickname, age_band, track, managed_by) values ('Z', '8-9', 'kid', $1) returning id`, [EVE]))[0].id
  await fails(user(RAY), call, [gulls, 'Coach Ray', ['def-quarters'], null, 80, '', 20000,
    JSON.stringify([{ learner_id: mia }, { learner_id: stranger }])])
  await fails(user(RAY), call, [gulls, 'Coach Ray', ['def-quarters'], null, 80, '', 20000, '[]'], /at least one/)
  // Warm-ups default on; a coach can turn them off and it's remembered.
  const off = (await rows(user(RAY), 'select send_assignment($1, $2, $3, $4, $5, $6, $7, $8, false) as id', [gulls, 'Coach Ray', ['def-quarters'], null, 80, '', 20000,
    JSON.stringify([{ learner_id: mia }])]))[0].id
  const flags = await rows(user(RAY), 'select id, include_warmups from assignment where id in ($1, $2)', [ok, off])
  assert.deepEqual(Object.fromEntries(flags.map(r => [r.id, r.include_warmups])), { [ok]: true, [off]: false })
  assert.equal((await rows(user(RAY), 'select count(*)::int as n from assignment'))[0].n, before + 1)
})

test('deleting a child removes all their cloud data', async () => {
  await as(user(RAY), 'delete from learner where id = $1', [leo])
  await db.exec('reset role')
  for (const t of ['event', 'consent', 'team_member', 'assignment_target']) {
    const r = await db.query(`select count(*)::int as n from ${t} where learner_id = $1`, [leo])
    assert.equal(r.rows[0].n, 0, t)
  }
  // A coach can't delete someone else's child: the delete matches nothing.
  assert.equal((await rows(user(RAY), 'delete from learner where id = $1 returning *', [mia])).length, 0)
  assert.equal((await rows(user(PAT), 'select * from learner where id = $1', [mia])).length, 1)
})
