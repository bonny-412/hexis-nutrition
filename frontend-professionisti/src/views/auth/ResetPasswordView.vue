<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { resetPassword } from '@/api/auth'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const route = useRoute()
const router = useRouter()
const token = typeof route.query.token === 'string' ? route.query.token : ''

const nuovaPassword = ref('')
const conferma = ref('')
const inCorso = ref(false)
const errori = ref<{ nuovaPassword?: string; conferma?: string }>({})

function erroreNuovaPassword(valore: string): string | undefined {
  if (!valore) return 'La nuova password è obbligatoria.'
  if (valore.length < 8) return 'La password deve avere almeno 8 caratteri.'
  return undefined
}

function erroreConferma(valore: string): string | undefined {
  if (!valore) return 'Ripeti la nuova password.'
  if (valore !== nuovaPassword.value) return 'Le due password non coincidono.'
  return undefined
}

// Come negli altri form: l'errore resta finché il campo non è corretto, poi sparisce subito.
function onNuovaPasswordInput(valore: string | number) {
  nuovaPassword.value = String(valore)
  if (errori.value.nuovaPassword) errori.value.nuovaPassword = erroreNuovaPassword(nuovaPassword.value)
  if (errori.value.conferma) errori.value.conferma = erroreConferma(conferma.value)
}

function onConfermaInput(valore: string | number) {
  conferma.value = String(valore)
  if (errori.value.conferma) errori.value.conferma = erroreConferma(conferma.value)
}

async function onSubmit() {
  errori.value = {
    nuovaPassword: erroreNuovaPassword(nuovaPassword.value),
    conferma: erroreConferma(conferma.value),
  }
  if (errori.value.nuovaPassword || errori.value.conferma) return
  inCorso.value = true
  try {
    await resetPassword(token, nuovaPassword.value)
    router.push({ name: 'login' })
  } catch (e) {
    if (e instanceof ApiError && e.status === 400) {
      toast.error('Il link non è più valido: richiedine uno nuovo.', {
        action: {
          label: 'Richiedi un nuovo link',
          onClick: () => router.push({ name: 'password-dimenticata' }),
        },
      })
    } else {
      toast.error('Errore imprevisto, riprova.')
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-(--bg)">
    <form class="w-full max-w-90" novalidate @submit.prevent="onSubmit">
      <h1 class="font-heading text-2xl italic text-(--fg)">Imposta una nuova password</h1>

      <div class="mb-3.5 mt-4 flex flex-col gap-1.5">
        <Label for="nuova-password" class="text-[13px] font-medium text-(--fg2)">Nuova password*</Label>
        <Input
          id="nuova-password" :model-value="nuovaPassword" type="password" autocomplete="new-password"
          :aria-invalid="!!errori.nuovaPassword" @update:model-value="onNuovaPasswordInput"
        />
        <p v-if="errori.nuovaPassword" class="text-xs font-medium text-(--danger)">{{ errori.nuovaPassword }}</p>
        <p v-else class="text-xs text-(--fg3)">Almeno 8 caratteri.</p>
      </div>

      <div class="mb-5 flex flex-col gap-1.5">
        <Label for="conferma-password" class="text-[13px] font-medium text-(--fg2)">Conferma password*</Label>
        <Input
          id="conferma-password" :model-value="conferma" type="password" autocomplete="new-password"
          :aria-invalid="!!errori.conferma" @update:model-value="onConfermaInput"
        />
        <p v-if="errori.conferma" class="text-xs font-medium text-(--danger)">{{ errori.conferma }}</p>
      </div>

      <Button type="submit" :disabled="inCorso" class="w-full">
        {{ inCorso ? 'Salvataggio…' : 'Imposta password' }}
      </Button>
    </form>
  </div>
</template>
