<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
import { toast } from 'vue-sonner'
import { Eye, EyeOff, Loader2 } from '@lucide/vue'
import { erroreEmail } from '@/utils/validators'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import AccessoLayout from '@/components/auth/AccessoLayout.vue'

const email = ref('')
const password = ref('')
const ricordami = ref(true)
const passwordVisibile = ref(false)
const inCorso = ref(false)
const errori = ref<{ email?: string; password?: string }>({})

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

function errorePassword(valore: string): string | undefined {
  return valore ? undefined : 'La password è obbligatoria.'
}

function onEmailInput(valore: string | number) {
  email.value = String(valore)
  if (errori.value.email) errori.value.email = erroreEmail(email.value)
}

function onPasswordInput(valore: string | number) {
  password.value = String(valore)
  if (errori.value.password) errori.value.password = errorePassword(password.value)
}

async function onSubmit() {
  if (inCorso.value) return
  errori.value = { email: erroreEmail(email.value), password: errorePassword(password.value) }
  if (errori.value.email || errori.value.password) return
  inCorso.value = true
  try {
    await auth.login(email.value, password.value, ricordami.value)
    const destinazione = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(destinazione)
  } catch (errore) {
    if (errore instanceof ApiError && errore.status === 401) {
      toast.error('Email o password non corrette. Riprova.')
    } else {
      toast.error('Servizio temporaneamente non raggiungibile. Riprova più tardi.')
    }
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AccessoLayout>
    <div class="mb-8">
      <h1 class="font-heading text-3xl italic text-(--fg)">Accedi</h1>
      <p class="mt-2 text-sm text-(--fg3)">Inserisci le tue credenziali per entrare nel tuo studio</p>
    </div>

    <form class="space-y-4" novalidate @submit.prevent="onSubmit">
      <!-- Campo Email -->
      <div class="space-y-1.5">
        <Label for="email" class="text-[13px] font-medium text-(--fg3)">Email</Label>
        <Input
          id="email"
          :model-value="email"
          type="email"
          :aria-invalid="!!errori.email"
          autocomplete="username"
          @update:model-value="onEmailInput"
          placeholder="nome@studio.it"
          class="h-11 rounded-xl border-(--bd) bg-(--surf) dark:bg-(--surf) px-3.5 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)"
        />
        <p v-if="errori.email" class="text-xs font-medium text-(--danger)">{{ errori.email }}</p>
      </div>

      <!-- Campo Password -->
      <div class="space-y-1.5">
        <div class="flex items-center justify-between">
          <Label for="password" class="text-[13px] font-medium text-(--fg3)">Password</Label>
          <router-link to="/password-dimenticata" class="text-xs font-semibold text-(--green) transition-colors hover:text-(--green-d)">
            Password dimenticata?
          </router-link>
        </div>
        <div class="relative flex items-center">
          <Input
            id="password"
            :model-value="password"
            :type="passwordVisibile ? 'text' : 'password'"
            :aria-invalid="!!errori.password"
            autocomplete="current-password"
            @update:model-value="onPasswordInput"
            placeholder="••••••••"
            class="h-11 w-full rounded-xl border-(--bd) bg-(--surf) dark:bg-(--surf) pl-3.5 pr-10 text-sm transition-all focus-visible:ring-2 focus-visible:ring-(--green)"
          />
          <button
            type="button"
            class="absolute right-3 text-(--fg3) transition-colors hover:text-(--fg)"
            :aria-label="passwordVisibile ? 'Nascondi password' : 'Mostra password'"
            @click="passwordVisibile = !passwordVisibile"
          >
            <EyeOff v-if="passwordVisibile" :size="18" />
            <Eye v-else :size="18" />
          </button>
        </div>
        <p v-if="errori.password" class="text-xs font-medium text-(--danger)">{{ errori.password }}</p>
      </div>

      <!-- Remember Me -->
      <div class="flex items-center gap-2 pt-1">
        <Checkbox id="ricordami" v-model="ricordami" class="rounded-md border-(--bd) data-[state=checked]:bg-(--green)" />
        <Label for="ricordami" class="cursor-pointer text-xs font-medium text-(--fg2)">
          Ricordami su questo dispositivo
        </Label>
      </div>

      <!-- Submit Button -->
      <Button
        type="submit"
        :disabled="inCorso"
        class="mt-2 h-11 w-full rounded-xl bg-(--green) text-sm font-semibold text-(--on-green) transition-all hover:bg-(--green-d) active:not-aria-[haspopup]:translate-y-0.5"
      >
        <Loader2 v-if="inCorso" :size="18" class="mr-2 animate-spin" />
        <span>{{ inCorso ? 'Accesso in corso…' : 'Accedi' }}</span>
      </Button>
    </form>
  </AccessoLayout>
</template>
