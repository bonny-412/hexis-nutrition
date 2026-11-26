<script setup lang="ts">
import type { DialogContentEmits, DialogContentProps } from 'reka-ui'

import type { HTMLAttributes } from 'vue'
import { XIcon } from '@lucide/vue'
import { reactiveOmit } from '@vueuse/core'
import {
  DialogClose,
  DialogContent,
  DialogPortal,
  useForwardPropsEmits,
} from 'reka-ui'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import DialogOverlay from './DialogOverlay.vue'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(defineProps<DialogContentProps & { class?: HTMLAttributes['class'], showCloseButton?: boolean }>(), {
  showCloseButton: true,
})
const emits = defineEmits<DialogContentEmits>()

const delegatedProps = reactiveOmit(props, 'class')

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <DialogPortal>
    <DialogOverlay />
    <!-- Adattamento Hexis: sotto i 640 px il dialog è un pannello che sale dal basso (bottom sheet),
      a tutta larghezza; da 640 px resta centrato come nell'originale shadcn. -->
    <DialogContent
      data-slot="dialog-content"
      v-bind="{ ...$attrs, ...forwarded }"
      :class="cn('fixed z-50 w-full outline-none bg-popover text-popover-foreground ring-1 ring-foreground/10 inset-x-0 bottom-0 max-h-[90dvh] rounded-t-2xl p-4 pb-[max(1rem,env(safe-area-inset-bottom))] duration-200 data-open:animate-in data-closed:animate-out data-open:slide-in-from-bottom data-closed:slide-out-to-bottom motion-reduce:animate-none sm:inset-x-auto sm:bottom-auto sm:top-1/2 sm:left-1/2 sm:max-h-[calc(100%-2rem)] sm:-translate-x-1/2 sm:-translate-y-1/2 sm:rounded-xl sm:pb-4 sm:duration-100 sm:data-open:[--tw-enter-translate-y:0] sm:data-closed:[--tw-exit-translate-y:0] sm:data-open:fade-in-0 sm:data-closed:fade-out-0 sm:data-open:zoom-in-95 sm:data-closed:zoom-out-95 flex flex-col gap-4 overflow-hidden text-sm sm:max-w-sm', props.class)"
      @pointer-down-outside.prevent
    >
      <slot />

      <DialogClose
        v-if="showCloseButton"
        data-slot="dialog-close"
        as-child
      >
        <Button variant="ghost" class="absolute top-2 right-2" size="icon-sm">
          <XIcon />
          <span class="sr-only">Chiudi</span>
        </Button>
      </DialogClose>
    </DialogContent>
  </DialogPortal>
</template>
