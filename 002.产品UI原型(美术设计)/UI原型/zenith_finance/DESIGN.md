---
name: Zenith Finance
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#434655'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#784b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#996100'
  on-tertiary-container: '#ffeedd'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
  success-growth: '#10B981'
  danger-expense: '#EF4444'
  warning-alert: '#F59E0B'
  bg-light: '#F8FAFC'
  bg-dark: '#0F172A'
  surface-dark: '#1E293B'
typography:
  display-hero:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-hero-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  numeric-data:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 24px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base-unit: 4px
  container-margin: 16px
  gutter: 12px
  touch-target-min: 44px
  card-padding: 20px
---

## Brand & Style

The design system is anchored in the concept of **Financial Clarity**. It targets modern professionals who seek a friction-less, high-utility tool for daily expense tracking. The brand personality is **utilitarian, reliable, and calm**, aiming to reduce the cognitive load often associated with money management.

We employ a **Corporate / Modern** aesthetic with a strong influence from **Minimalism**. The interface prioritizes data legibility through generous whitespace, a structured grid, and a focus on "Progressive Disclosure"—only showing the user what they need at the exact moment they need it. The UI feels "light" and "airy," using subtle shadows and high-contrast typography to create a sense of order and prosperity.

## Colors

The palette is strategically split between **Trust (Blue)** and **Growth (Green)**. 

- **Primary (Finance Blue):** Used for primary actions, navigation states, and brand-heavy elements. It evokes professionalism and stability.
- **Secondary (Growth Green):** Used to represent income, savings progress, and positive financial trends.
- **Semantic Colors:** We use a strict traffic-light system. Red is reserved exclusively for expenses, over-budget alerts, and deletions. Orange serves as a cautionary state for budget limits nearing 80% capacity.
- **Adaptability:** The design system supports a native Dark Mode. In Dark Mode, the background shifts to a deep navy (`#0F172A`) rather than pure black to maintain depth, with surfaces elevated via `#1E293B`. All pairings are tested to exceed WCAG 2.1 AA contrast ratios (4.5:1).

## Typography

We use **Inter** for its exceptional legibility and neutral tone. It excels in displaying numerical data, which is core to this design system.

- **Numerical Clarity:** All numeric displays must use `tabular-nums` (tnum) to ensure that columns of figures align perfectly in transaction lists.
- **Hierarchy:** Display-Hero is used exclusively for the main balance or monthly total. Group headings (dates) use `label-caps` to provide a clear structural break without competing for attention with transaction amounts.
- **Accessibility:** Typography scales fluidly. On mobile, we reduce the display size to prevent awkward line breaks while maintaining a minimum tap-target-friendly body size of 16px.

## Layout & Spacing

The layout follows a **Mobile-First, Fluid Grid** philosophy. 

- **Grid Model:** A 4-column grid for mobile (375px-414px) and a 12-column grid for desktop. Margins are fixed at 16px on mobile to maximize horizontal space for transaction details.
- **Rhythm:** An 8pt linear scale is used for all spatial relationships. 
- **The "Thumb Zone":** Critical actions (The "Add" button and Tab Bar) are placed within the bottom 25% of the screen.
- **Responsive Reflow:** On desktop, the central "Overview Card" and "Transaction List" are centered with a max-width of 768px to ensure the user's eye doesn't have to travel too far horizontally to scan data.

## Elevation & Depth

Visual hierarchy is established through **Tonal Layering** and **Ambient Shadows**.

- **Surface Tiers:**
    - **Level 0 (Base):** The page background (`#F8FAFC`).
    - **Level 1 (Cards):** Pure white background with a very soft, diffused shadow (10% opacity, 12px blur, 4px Y-offset).
    - **Level 2 (Navigation):** The bottom Tab Bar uses a background blur (15px) with a subtle top border (`1px solid rgba(0,0,0,0.05)`) to separate it from scrolling content.
- **Overlays:** The "Quick Record" panel slides from the bottom with a 40% dimmed backdrop, signaling a temporary shift in focus.

## Shapes

The shape language is **Rounded**, conveying a modern and approachable feel. 

- **Primary Containers:** Cards and input fields use a **0.5rem (8px)** corner radius.
- **Interactive Elements:** Buttons and Category Icons use more pronounced rounding (**1rem**) to make them feel more "touchable" and distinct from static layout boxes. 
- **The "Add" Button:** A perfect circle is used for the primary floating action button to denote its unique importance.

## Components

- **Buttons:** Primary buttons use a solid Finance Blue fill. Secondary buttons use a subtle ghost style with a 1px border. All buttons must maintain a minimum height of **44px**.
- **Cards (Overview):** Use `card-padding: 20px`. The main "Monthly Spending" card should feature the largest typography and a subtle Growth Green accent bar to indicate its summary status.
- **Category Chips:** Small circular icons with 12px labels underneath. Used in a grid for the "Quick Record" flow. 
- **Transaction Lists:** Each row has a minimum height of 64px. Icons are placed on the far left, followed by category/remark, and the amount is right-aligned using `numeric-data` tokens.
- **Inputs:** Use `type="tel"` for amount entry to trigger the numeric keypad. Input fields should have a distinct focus state using a 2px Primary Blue glow.
- **Quick Record Panel:** A bottom-sheet component that occupies 70% of the screen height, allowing for rapid one-handed category selection and amount entry.