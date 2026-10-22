# App keep rules for R8 (ADR-013). Libraries bring their own; add here only what R8
# cannot see: classes reached by reflection or by name.
#
# Content and save models are @Serializable and resolved through generated serializers,
# which kotlinx.serialization's bundled rules keep. Nothing in the app uses reflection.
