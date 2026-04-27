---
name: Pro-Performance Audio
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#bdc8d0'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#889299'
  outline-variant: '#3e484f'
  surface-tint: '#75d1ff'
  primary: '#9adbff'
  on-primary: '#003548'
  primary-container: '#4fc3f7'
  on-primary-container: '#004e69'
  inverse-primary: '#006688'
  secondary: '#71d7cd'
  on-secondary: '#003733'
  secondary-container: '#32a097'
  on-secondary-container: '#00302c'
  tertiary: '#f8c0ff'
  on-tertiary: '#4b1b58'
  tertiary-container: '#dfa2e8'
  on-tertiary-container: '#663471'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c2e8ff'
  primary-fixed-dim: '#75d1ff'
  on-primary-fixed: '#001e2b'
  on-primary-fixed-variant: '#004d67'
  secondary-fixed: '#8ef4e9'
  secondary-fixed-dim: '#71d7cd'
  on-secondary-fixed: '#00201d'
  on-secondary-fixed-variant: '#00504a'
  tertiary-fixed: '#fdd6ff'
  tertiary-fixed-dim: '#efb1f9'
  on-tertiary-fixed: '#340141'
  on-tertiary-fixed-variant: '#643370'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  technical-data:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.05em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-edge: 16px
---

## Brand & Style

This design system is engineered for the high-pressure environment of live music performance. The brand personality is **technical, precise, and high-fidelity**, prioritizing rapid visual scanning and tactile confidence. 

The aesthetic style is a hybrid of **Modern Technical** and **Glassmorphism**. It utilizes the structural logic of Material You but introduces specialized, high-density controls tailored for audio manipulation. The UI evokes the feeling of a premium hardware controller—expensive, responsive, and reliable—while maintaining the sleekness of a modern mobile OS. Visual elements are designed to be "glanceable," ensuring that critical performance data like BPM and pitch are legible even in dark, strobing club environments.

## Colors

The color palette is strictly functional, using hue to separate the two primary performance channels. **Deck A (Blue)** and **Deck B (Teal)** serve as the anchor points for the user's mental model, ensuring they always know which side of the mix they are manipulating. 

The background and surfaces use a tiered dark-gray scale to reduce eye strain and provide a deep canvas for the vibrant accent colors to pop. The **Tertiary Purple** is reserved for creative triggers (Sample Pads), while the **Error/Kill Red** provides immediate visual feedback for destructive actions or peak clipping. Transparency is used systematically for text hierarchy to maintain a clean visual stack without introducing unnecessary new hues.

## Typography

This design system utilizes a dual-font strategy. **Space Grotesk** is used for all technical data, headlines, and labels. Its geometric, slightly futuristic character reinforces the "technical instrument" feel and provides excellent legibility for numerical values like BPM, timestamps, and pitch percentages.

**Inter** is employed for body copy, settings, and lists. Its neutral, utilitarian design ensures that secondary information stays out of the way of the performance interface while remaining highly readable at smaller sizes. Technical labels should frequently use uppercase styling with increased letter spacing to maximize clarity on dense control panels.

## Layout & Spacing

The layout follows a **fluid, high-density grid** model. Because DJ software requires many simultaneous controls, the spacing rhythm is tight, built on an 8px baseline grid to maximize screen real estate on mobile devices.

Margins and gutters are kept at 16px to provide a comfortable "safe zone" for thumb interaction at the screen edges, but internal component spacing (between knobs and faders) can drop to 8px or 4px to ensure all critical tools are accessible on a single view. The layout logic prioritizes a symmetrical split for the two decks, with a central "mixer" section that anchors the vertical axis of the app.

## Elevation & Depth

Hierarchy is conveyed through **Tonal Layering** and **Low-Contrast Outlines**. In a dark performance environment, heavy drop shadows can create "muddiness." Instead, this design system uses the following tiers:

1.  **Background (#121212):** The base canvas.
2.  **Surface (#1E1E1E):** For main deck containers and the mixer track.
3.  **Surface Variant (#2A2A2A):** For interactive elements like pads, buttons, and fader tracks.
4.  **Interactive Glow:** Active states (like a 'Play' button or 'Cue' point) utilize a subtle outer glow using the deck's primary accent color (Blue or Teal) to simulate physical LED illumination.

Borders are 1px thick with low opacity (rgba(255,255,255,0.1)) to define edges without adding visual weight.

## Shapes

The shape language is diverse, using varied corner radii to distinguish between different types of functional zones. 

- **Cards (12px):** Used for large container blocks like Deck A/B enclosures or Library sections.
- **Sample Pads (6px):** Designed to look like physical MPC-style pads; the tighter radius suggests a smaller, clickable surface area.
- **Waveforms (5px):** Subtle rounding on the edges of the waveform display container to soften the technical data.
- **Chips & Small Controls (4px):** Used for toggles (Sync, Quantize) to maintain a crisp, professional look.

Consistent use of these specific radii allows the user to subconsciously categorize elements by their shape before even reading the label.

## Components

### Buttons & Toggles
Buttons should have a high-contrast state. Performance-critical buttons (Play/Pause, Cue) should be larger than secondary controls. Use the accent color for the 'active' state and Surface Variant for the 'inactive' state.

### Faders & Knobs
Vertical faders for volume and pitch should use a track color of `SurfaceVariant` with a `Primary` or `Secondary` accent for the "filled" portion. The fader handle should be a prominent, 12px rounded rectangle for tactile ease.

### Sample Pads
Arranged in a grid, these pads use the `Tertiary` (Purple) color. When triggered, the pad should flash with 100% opacity, then settle into a 30% opacity "active" glow if the sample is looping.

### Waveforms
The main visual focus. Use the Deck A/B accent colors for the waveform peaks. The background of the waveform should be `Background` (#121212) to ensure the peaks are the highest contrast element on the screen.

### Lists (Library)
Track lists should use `Surface` for the row container. Metadata (Artist, Key) should use `Text Muted`, while the Track Title uses `Text Primary`. Selected tracks are highlighted with a left-edge border in the deck's color.