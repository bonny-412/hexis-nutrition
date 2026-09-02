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

async function onSubmit() {
  if (nuovaPassword.value.length < 8) {
    toast.error('La password deve avere almeno 8 caratteri.')
    return
  }
  if (nuovaPassword.value !== conferma.value) {
    toast.error('Le due password non coincidono.')
    return
  }
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
    <form class="w-full max-w-90" @submit.prevent="onSubmit">
      <h1 class="font-heading text-2xl italic text-(--fg)">Imposta una nuova password</h1>

      <div class="mb-3.5 mt-4 flex flex-col gap-1.5">
        <Label for="nuova-password" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nuova password</Label>
        <Input id="nuova-password" v-model="nuovaPassword" type="password" required />
      </div>

      <div class="mb-5 flex flex-col gap-1.5">
        <Label for="conferma-password" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Conferma password</Label>
        <Input id="conferma-password" v-model="conferma" type="password" required />
      </div>

      <Button type="submit" :disabled="inCorso" class="w-full">
        {{ inCorso ? 'Salvataggio…' : 'Imposta password' }}
      </Button>
    </form>
  </div>
</template>
