<script setup lang="ts">
import { computed } from 'vue'
import { DateFormatter, getLocalTimeZone, parseDate, type DateValue } from '@internationalized/date'
import { Calendar as CalendarIcon } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  id?: string
}>(), {
  modelValue: '',
  placeholder: 'Seleziona una data',
})

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const formatter = new DateFormatter('it-IT', { dateStyle: 'long' })

const valore = computed<DateValue | undefined>({
  get: () => (props.modelValue ? parseDate(props.modelValue) : undefined),
  set: (data) => emit('update:modelValue', data ? data.toString() : ''),
})

const testoVisualizzato = computed(() =>
  valore.value ? formatter.format(valore.value.toDate(getLocalTimeZone())) : props.placeholder,
)
</script>

<template>
  <Popover>
    <PopoverTrigger as-child>
      <Button
        :id="id"
        type="button"
        variant="outline"
        :class="cn(
          'w-full justify-start border-input bg-transparent px-2.5 py-1 text-left text-base font-normal hover:bg-transparent dark:bg-input/30 md:text-sm',
          !valore && 'text-muted-foreground',
        )"
      >
        <CalendarIcon :size="16" class="mr-2 shrink-0" />
        {{ testoVisualizzato }}
      </Button>
    </PopoverTrigger>
    <PopoverContent class="w-auto overflow-hidden p-0">
      <Calendar 
        v-model="valore"
        locale="it"
        layout="month-and-year"
        initial-focus 
      />
    </PopoverContent>
  </Popover>
</template>