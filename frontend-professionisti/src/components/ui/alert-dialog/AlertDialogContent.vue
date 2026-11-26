<script setup lang="ts">
import type { AlertDialogContentEmits, AlertDialogContentProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import {
  AlertDialogContent,
  AlertDialogOverlay,
  AlertDialogPortal,
  useForwardPropsEmits,
} from 'reka-ui'
import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<AlertDialogContentProps & {
    class?: HTMLAttributes['class']
    size?: 'default' | 'sm'
  }>(),
  {
    size: 'default',
  },
)
const emits = defineEmits<AlertDialogContentEmits>()

const delegatedProps = reactiveOmit(props, 'class', 'size')

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <AlertDialogPortal>
    <AlertDialogOverlay
      data-slot="alert-dialog-overlay"
      class="data-open:animate-in data-closed:animate-out data-closed:fade-out-0 data-open:fade-in-0 bg-black/10 duration-100 supports-backdrop-filter:backdrop-blur-xs fixed inset-0 z-50"
    />
    <AlertDialogContent
      data-slot="alert-dialog-content"
      :data-size="size"
      v-bind="{ ...$attrs, ...forwarded }"
      :class="
        cn(
          // Adattamento Hexis: pannello dal basso sotto i 640 px, come DialogContent; da 640 px centrato.
          'fixed z-50 w-full outline-none bg-popover text-popover-foreground ring-1 ring-foreground/10 inset-x-0 bottom-0 max-h-[90dvh] rounded-t-2xl p-4 pb-[max(1rem,env(safe-area-inset-bottom))] duration-200 data-open:animate-in data-closed:animate-out data-open:slide-in-from-bottom data-closed:slide-out-to-bottom motion-reduce:animate-none sm:inset-x-auto sm:bottom-auto sm:top-1/2 sm:left-1/2 sm:max-h-[calc(100%-2rem)] sm:-translate-x-1/2 sm:-translate-y-1/2 sm:rounded-xl sm:pb-4 sm:duration-100 sm:data-open:[--tw-enter-translate-y:0] sm:data-closed:[--tw-exit-translate-y:0] sm:data-open:fade-in-0 sm:data-closed:fade-out-0 sm:data-open:zoom-in-95 sm:data-closed:zoom-out-95 group/alert-dialog-content grid gap-4 sm:data-[size=default]:max-w-sm sm:data-[size=sm]:max-w-xs',
          props.class,
        )
      "
    >
      <slot />
    </AlertDialogContent>
  </AlertDialogPortal>
</template>
