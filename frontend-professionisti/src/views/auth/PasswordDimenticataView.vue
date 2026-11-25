<script setup lang="ts">
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { richiediResetPassword } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ArrowLeft, Loader2, MailCheck } from '@lucide/vue'
import AccessoLayout from '@/components/auth/AccessoLayout.vue'
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
  <AccessoLayout>
    <template v-if="inviato">
      <div class="flex flex-col items-start gap-4" role="status" data-test="reset-inviato">
        <span class="flex size-12 items-center justify-center rounded-full bg-(--mint) text-(--green-d)" aria-hidden="true">
          <MailCheck :size="24" />
        </span>
        <div>
          <h1 class="font-heading text-3xl italic text-(--fg)">Controlla la posta</h1>
          <p class="mt-2 text-sm text-(--fg3)">
            Se l'indirizzo esiste, riceverai un'email con le istruzioni per reimpostare la password.
          </p>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="mb-8">
        <h1 class="font-heading text-3xl italic text-(--fg)">Password dimenticata</h1>
        <p class="mt-2 text-sm text-(--fg3)">Inserisci l'email dello studio: ti mandiamo un link per sceglierne una nuova.</p>
      </div>

      <form class="space-y-4" novalidate @submit.prevent="onSubmit">
        <div class="space-y-1.5">
          <Label for="email" class="text-[13px] font-medium text-(--fg3)">Email</Label>
          <Input
            id="email" :model-value="email" type="email" autocomplete="username" placeholder="nome@studio.it"
            :aria-invalid="!!errore" class="h-11 rounded-xl border-(--bd) bg-(--surf) dark:bg-(--surf) px-3.5 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)" @update:model-value="onEmailInput"
          />
          <p v-if="errore" class="text-xs font-medium text-(--danger)">{{ errore }}</p>
        </div>

        <Button type="submit" :disabled="inCorso" class="mt-2 h-11 w-full rounded-xl text-sm font-semibold">
          <Loader2 v-if="inCorso" :size="18" class="animate-spin" />
          {{ inCorso ? 'Invio in corso…' : 'Invia istruzioni' }}
        </Button>
      </form>
    </template>

    <router-link to="/login" class="group mt-6 inline-flex items-center gap-1.5 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)">
      <ArrowLeft :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:-translate-x-0.5" />
      <span>Torna al login</span>
    </router-link>
  </AccessoLayout>
</template>
