Text

Primary text → textPrimary (#F0F0FA)
Secondary/artist text → textSecondary (#CAC4D8)
Muted labels, timestamps → textMuted (#948FA8)
Dimmed section labels → textDim (#4A4A5E)

Backgrounds

Screen background → backgroundPrimary (#0A0A0F)
Cards, bottom sheets → backgroundSecondary (#13131A)
Elevated surfaces, chips → backgroundSurface (#2A2A38)
Now playing row → backgroundElevated (#1C1C26)

Icons

Active/accent icons → iconActive (#9D8FFF)
Default inactive icons → iconSecondary (#CAC4D8)
Dimmed/disabled icons → iconMuted (#948FA8)

Accent elements

Play button background → accentPrimary (#9D8FFF)
Play button icon → onAccent (#0F0F18)
Progress bar fill → accentDark → accentPrimary gradient
Active dot indicator → accentPrimary
"Queued" badge background → accentContainer (#3D2EAD)
"Queued" badge text → onAccentContainer (#9D8FFF)

Autoplay/tertiary elements

"Auto" badge, autoplay section header → rose (#F0A8BC)
"Auto" badge background → roseContainer (#4A1A2E)
Text on rose elements → onRose (#3A0A1E)

Dividers/borders

Subtle dividers → dividerSubtle (#0FFFFFFF)
Visible dividers → divider (#2A2A38)
Borders at low alpha → divider at 60% alpha

One fix — onRose in dark is #3A0A1E which is very dark, good for text on the rose badge background. But in light mode onRose is #FFFFFF which is correct since rose in light is a dark pink (#BF5A7A).
