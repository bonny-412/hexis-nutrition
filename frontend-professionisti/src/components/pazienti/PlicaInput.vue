<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { filtraDecimaleItaliano, numeroItalianoOpzionale } from '@/utils/validators'

const props = defineProps<{
  id: string
  label: string
  modelValue: string
  errore?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [valore: string]
}>()

const tripla = ref(false)
const misura1 = ref('')
const misura2 = ref('')
const misura3 = ref('')

const mediaCalcolata = computed(() => {
  const n1 = numeroItalianoOpzionale(misura1.value)
  const n2 = numeroItalianoOpzionale(misura2.value)
  const n3 = numeroItalianoOpzionale(misura3.value)
  if (n1 === undefined || n2 === undefined || n3 === undefined) return null
  return (n1 + n2 + n3) / 3
})

watch(mediaCalcolata, (media) => {
  if (!tripla.value) return
  emit('update:modelValue', media !== null ? media.toFixed(2).replace('.', ',') : '')
})

function onSingoloInput(valore: string | number) {
  emit('update:modelValue', filtraDecimaleItaliano(String(valore)))
}

function onMisura1Input(valore: string | number) {
  misura1.value = filtraDecimaleItaliano(String(valore))
}

function onMisura2Input(valore: string | number) {
  misura2.value = filtraDecimaleItaliano(String(valore))
}

function onMisura3Input(valore: string | number) {
  misura3.value = filtraDecimaleItaliano(String(valore))
}

function onTriplaChange(valore: boolean) {
  tripla.value = valore
  if (!valore) {
    misura1.value = ''
    misura2.value = ''
    misura3.value = ''
  } else {
    emit('update:modelValue', '')
  }
}
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <div class="flex items-center justify-between gap-2">
      <Label :for="id" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ label }} (mm)</Label>
      <label class="flex items-center gap-1.5 text-xs text-(--fg3)">
        <Checkbox :id="`${id}-tripla`" :model-value="tripla" @update:model-value="onTriplaChange" />
        Tripla misurazione
      </label>
    </div>

    <Input v-if="!tripla" :id="id" :model-value="modelValue" @update:model-value="onSingoloInput" type="text" inputmode="decimal" placeholder="Es. 12,50" :aria-invalid="!!errore" />

    <div v-else class="grid grid-cols-3 gap-2">
      <Input :id="`${id}-m1`" :model-value="misura1" @update:model-value="onMisura1Input" type="text" inputmode="decimal" placeholder="Misura 1" :aria-invalid="!!errore" />
      <Input :id="`${id}-m2`" :model-value="misura2" @update:model-value="onMisura2Input" type="text" inputmode="decimal" placeholder="Misura 2" :aria-invalid="!!errore" />
      <Input :id="`${id}-m3`" :model-value="misura3" @update:model-value="onMisura3Input" type="text" inputmode="decimal" placeholder="Misura 3" :aria-invalid="!!errore" />
    </div>
    <p v-if="tripla && mediaCalcolata !== null" class="text-xs text-(--fg3)">Media calcolata: {{ mediaCalcolata.toFixed(2).replace('.', ',') }} mm</p>
    <p v-if="errore" class="text-xs font-medium text-(--danger)">{{ errore }}</p>
  </div>
</template>
