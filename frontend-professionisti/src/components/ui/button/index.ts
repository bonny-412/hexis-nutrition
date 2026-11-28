import type { VariantProps } from 'class-variance-authority'
import { cva } from 'class-variance-authority'

export { default as Button } from './Button.vue'

export const buttonVariants = cva(
  'focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:aria-invalid:border-destructive/50 rounded-lg border border-transparent bg-clip-padding text-sm font-medium focus-visible:ring-3 aria-invalid:ring-3 active:translate-y-0.5 [&_svg:not([class*=size-])]:size-4 group/button inline-flex shrink-0 items-center justify-center whitespace-nowrap transition-all outline-none select-none disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        // Hover: petrolio più scuro (--green-d), non un'opacità: il testo bianco mantiene il contrasto.
        default: 'bg-primary text-primary-foreground hover:bg-(--green-d) aria-expanded:bg-(--green-d)',
        outline: 'border-border bg-background hover:bg-muted hover:text-foreground dark:bg-input/30 dark:border-input dark:hover:bg-input/50 aria-expanded:bg-muted aria-expanded:text-foreground',
        secondary: 'bg-secondary text-secondary-foreground hover:bg-[color-mix(in_srgb,var(--mint),var(--green)_12%)] aria-expanded:bg-secondary aria-expanded:text-secondary-foreground',
        ghost: 'hover:bg-muted hover:text-foreground dark:hover:bg-muted/50 aria-expanded:bg-muted aria-expanded:text-foreground',
        // Conferma di un'azione distruttiva (dialog "Elimina…"): rosso pieno, deve pesare più di "Annulla".
        // Hover come il primario: rosso più scuro (--danger-d), non un'opacità.
        destructive: 'bg-(--danger) text-(--on-danger) hover:bg-(--danger-d) aria-expanded:bg-(--danger-d) focus-visible:border-(--danger) focus-visible:ring-destructive/30 dark:focus-visible:ring-destructive/40',
        'destructive-outline': 'border-destructive/30 text-destructive bg-background hover:bg-destructive/10 hover:border-destructive focus-visible:ring-destructive/20 dark:focus-visible:ring-destructive/40 dark:border-destructive/40 dark:hover:bg-destructive/20',
        // Pulsante testo: niente sottolineatura né effetto di pressione (quello è solo dei pulsanti azione).
        link: 'text-primary hover:text-(--green-d) active:translate-y-0',
        neutral: 'bg-card text-foreground border-border [a]:hover:bg-muted hover:bg-muted aria-expanded:bg-muted',
        // Rimozione ripetuta su molte righe (es. "Rimuovi alimento"): neutra a riposo, rossa solo al passaggio.
        rimuovi: 'hover:bg-destructive/10 hover:text-destructive focus-visible:text-destructive focus-visible:ring-destructive/20 dark:hover:bg-destructive/20',
        'destructive-ghost': 'text-destructive hover:bg-destructive/10 focus-visible:ring-destructive/20 dark:focus-visible:ring-destructive/40 hover:text-destructive dark:hover:bg-destructive/20 aria-expanded:bg-destructive/10',
        'ghost-primary': 'text-(--green) hover:bg-(--mint) hover:text-(--green-d) dark:hover:bg-(--mint)/20 aria-expanded:bg-(--mint)',
        // Sui blocchi petrolio (hero): pillola secondaria e azione principale lime, una per blocco.
        hero: 'border-transparent bg-(--hero-pill) text-(--hero-pill-fg) ring-1 ring-(--hero-pill-ring) hover:bg-(--hero-pill-hover) aria-expanded:bg-(--hero-pill-hover)',
        accent: 'bg-(--lime) text-(--on-lime) hover:bg-(--lime-d) aria-expanded:bg-(--lime-d)',
        // Filtri a pillola delle liste e dell'Agenda: lo stato acceso viene da aria-pressed.
        chip: 'rounded-full border-(--bd2) bg-(--surf) text-xs font-bold text-(--fg2) hover:border-(--sage) aria-pressed:border-(--sage) aria-pressed:bg-(--mint) aria-pressed:text-(--green-d) active:translate-y-0',
        // Azione secondaria in coda ai filtri ("Pulisci filtri").
        tratteggiato: 'border-dashed border-(--dash) text-xs font-bold text-(--fg3) hover:border-(--fg4) hover:text-(--fg)',
        'destructive-neutral':'border-border bg-card text-destructive hover:bg-destructive/10 focus-visible:border-destructive/40 focus-visible:ring-destructive/20 dark:hover:bg-destructive/15 dark:focus-visible:ring-destructive/40',
      },
      size: {
        'default': 'h-8 gap-1.5 px-2.5 has-data-[icon=inline-end]:pr-2 has-data-[icon=inline-start]:pl-2',
        'xs': 'h-6 gap-1 rounded-[min(var(--radius-md),10px)] px-2 text-xs in-data-[slot=button-group]:rounded-lg has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*=size-])]:size-3',
        'sm': 'h-7 gap-1 rounded-[min(var(--radius-md),12px)] px-2.5 text-[0.8rem] in-data-[slot=button-group]:rounded-lg has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*=size-])]:size-3.5',
        'lg': 'h-9 gap-1.5 px-2.5 has-data-[icon=inline-end]:pr-2 has-data-[icon=inline-start]:pl-2',
        'icon': 'size-8',
        'icon-xs': 'size-6 rounded-[min(var(--radius-md),10px)] in-data-[slot=button-group]:rounded-lg [&_svg:not([class*=size-])]:size-3',
        'icon-sm': 'size-7 rounded-[min(var(--radius-md),12px)] in-data-[slot=button-group]:rounded-lg',
        'icon-lg': 'size-9',
        'chip': 'h-auto gap-1.5 px-3 py-1.5',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)
export type ButtonVariants = VariantProps<typeof buttonVariants>
