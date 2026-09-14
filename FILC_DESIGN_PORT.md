# Legacy Filc visual design port

This branch recreates the visual language of the archived Filc mobile app in
Mergen's Compose Multiplatform UI.

Reference projects:

- `filc/mobile-archive`: mobile screens, navigation, filters, panels
- `filc/naplo-archive`: color system and theme

Ported visual rules:

- Filc accent: `#20AC9B`
- light background: `#F4F9FF`
- light panels: `#FFFFFF`
- dark background: `#000000`
- dark panels: `#141516`
- 16 dp panel corners
- soft panel shadows in light mode
- rounded selected navigation item
- pill-shaped filters
- large 32 sp bold page headings
- greeting + profile-style top bar
- icon-first bottom navigation without text wrapping

The original Flutter code was not dropped into the Kotlin project directly.
The UI is reimplemented in Compose Multiplatform while preserving the existing
Mergen data/auth/navigation logic.

The old Filc project used Montserrat. This port intentionally uses the platform
font stack instead of redistributing font binaries.

See `THIRD_PARTY_NOTICES.md` for BSD 3-Clause attribution.
