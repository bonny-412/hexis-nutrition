<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { resetPassword } from '@/api/auth'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ArrowLeft, Loader2 } from '@lucide/vue'
import AccessoLayout from '@/components/auth/AccessoLayout.vue'

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
  <AccessoLayout>
    <div class="mb-8">
      <h1 class="font-heading text-3xl italic text-(--fg)">Imposta una nuova password</h1>
      <p class="mt-2 text-sm text-(--fg3)">La userai da ora in poi per accedere allo studio.</p>
    </div>

    <form class="space-y-4" novalidate @submit.prevent="onSubmit">
      <div class="space-y-1.5">
        <Label for="nuova-password" class="text-[13px] font-medium text-(--fg3)">Nuova password</Label>
        <Input
          id="nuova-password" :model-value="nuovaPassword" type="password" autocomplete="new-password"
          :aria-invalid="!!errori.nuovaPassword" class="h-11 rounded-xl border-(--bd) bg-(--surf) dark:bg-(--surf) px-3.5 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)" @update:model-value="onNuovaPasswordInput"
        />
        <p v-if="errori.nuovaPassword" class="text-xs font-medium text-(--danger)">{{ errori.nuovaPassword }}</p>
        <p v-else class="text-xs text-(--fg3)">Almeno 8 caratteri.</p>
      </div>

      <div class="space-y-1.5">
        <Label for="conferma-password" class="text-[13px] font-medium text-(--fg3)">Conferma password</Label>
        <Input
          id="conferma-password" :model-value="conferma" type="password" autocomplete="new-password"
          :aria-invalid="!!errori.conferma" class="h-11 rounded-xl border-(--bd) bg-(--surf) dark:bg-(--surf) px-3.5 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)" @update:model-value="onConfermaInput"
        />
        <p v-if="errori.conferma" class="text-xs font-medium text-(--danger)">{{ errori.conferma }}</p>
      </div>

      <Button type="submit" :disabled="inCorso" class="mt-2 h-11 w-full rounded-xl text-sm font-semibold">
        <Loader2 v-if="inCorso" :size="18" class="animate-spin" />
        {{ inCorso ? 'Salvataggio…' : 'Imposta password' }}
      </Button>
    </form>

    <router-link to="/login" class="group mt-6 inline-flex items-center gap-1.5 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)">
      <ArrowLeft :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:-translate-x-0.5" />
      <span>Torna al login</span>
    </router-link>
  </AccessoLayout>
</template>
