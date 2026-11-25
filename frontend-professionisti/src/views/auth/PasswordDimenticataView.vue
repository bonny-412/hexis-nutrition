<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { richiediResetPassword } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ArrowLeft } from '@lucide/vue'
import { erroreEmail } from '@/utils/validators'

const email = ref('')
const errore = ref<string | undefined>()
const inviato = ref(false)
const inCorso = ref(false)

function onEmailInput(valore: string | number) {
  email.value = String(valore)
  if (errore.value) errore.value = erroreEmail(email.value)
}

async function onSubmit() {
  if (inCorso.value) return
  errore.value = erroreEmail(email.value)
  if (errore.value) return
  inCorso.value = true
  try {
    await richiediResetPassword(email.value)
    inviato.value = true
  } catch {
    toast.error('Errore di rete, riprova.')
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-(--bg)">
    <div class="w-full max-w-90">
      <h1 class="font-heading text-2xl italic text-(--fg)">Password dimenticata</h1>

      <p v-if="inviato" class="mt-4 text-sm text-(--fg2)">
        Se l'indirizzo esiste, riceverai un'email con le istruzioni per reimpostare la password.
      </p>

      <form v-else class="mt-4" novalidate @submit.prevent="onSubmit">
        <div class="mb-5 flex flex-col gap-1.5">
          <Label for="email" class="text-[13px] font-medium text-(--fg2)">Email*</Label>
          <Input
            id="email" :model-value="email" type="email" autocomplete="username" placeholder="nome@studio.it"
            :aria-invalid="!!errore" @update:model-value="onEmailInput"
          />
          <p v-if="errore" class="text-xs font-medium text-(--danger)">{{ errore }}</p>
        </div>

        <Button type="submit" :disabled="inCorso" class="w-full">
          {{ inCorso ? 'Invio in corso…' : 'Invia istruzioni' }}
        </Button>
      </form>

      <router-link to="/login" class="group mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)">
        <ArrowLeft :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:-translate-x-0.5" />
        <span>Torna al login</span>
      </router-link>
    </div>
  </div>
</template>
