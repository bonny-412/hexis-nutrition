<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Eye, EyeOff, Loader2, LogIn, LogOut } from '@lucide/vue'
import { ApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

// Sessione scaduta mentre si lavora (bug S5 della verifica 3): la password si chiede qui, sopra la pagina, così
// i dati non ancora salvati restano nei campi. Dopo l'accesso si torna alla pagina e si salva di nuovo.
// Non si chiude con Esc né cliccando fuori: senza sessione la pagina non può salvare nulla.
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const password = ref('')
const passwordVisibile = ref(false)
const inCorso = ref(false)
const errore = ref<string | null>(null)

const email = computed(() => auth.professionista?.email ?? '')

watch(() => auth.sessioneScaduta, (scaduta) => {
  if (!scaduta) return
  password.value = ''
  passwordVisibile.value = false
  errore.value = null
})

async function onSubmit() {
  if (inCorso.value) return
  if (!password.value) {
    errore.value = 'La password è obbligatoria.'
    return
  }
  inCorso.value = true
  errore.value = null
  try {
    await auth.riaccedi(password.value)
    password.value = ''
  } catch (e) {
    errore.value = e instanceof ApiError && e.status === 401
      ? 'Password non corretta.'
      : 'Server non raggiungibile. Riprova tra poco.'
  } finally {
    inCorso.value = false
  }
}

/** Rinuncia: si va al login. Se la pagina ha modifiche non salvate, la sua conferma di uscita lo chiede prima. */
function esci() {
  const pagina = route.fullPath
  auth.logout()
  void router.push({ name: 'login', query: { redirect: pagina } })
}
</script>

<template>
  <AlertDialog :open="auth.sessioneScaduta">
    <AlertDialogContent data-test="sessione-scaduta" @escape-key-down.prevent>
      <AlertDialogHeader>
        <AlertDialogTitle>Sessione scaduta</AlertDialogTitle>
        <AlertDialogDescription>
          Per sicurezza l'accesso dura 8 ore. Inserisci di nuovo la password: quello che hai scritto su questa pagina
          resta com'è, e dopo potrai salvarlo.
        </AlertDialogDescription>
      </AlertDialogHeader>

      <form id="form-sessione-scaduta" class="space-y-1.5" novalidate @submit.prevent="onSubmit">
        <!-- L'email serve ai gestori di password per proporre la credenziale giusta. -->
        <input type="email" name="email" autocomplete="username" :value="email" class="sr-only" tabindex="-1" aria-hidden="true" readonly />
        <Label for="sessione-scaduta-password" class="text-[13px] font-medium text-(--fg3)">
          Password di {{ email }}
        </Label>
        <div class="relative flex items-center">
          <Input
            id="sessione-scaduta-password"
            v-model="password"
            :type="passwordVisibile ? 'text' : 'password'"
            autocomplete="current-password"
            autofocus
            :aria-invalid="!!errore"
            :aria-describedby="errore ? 'sessione-scaduta-errore' : undefined"
            class="h-11 w-full rounded-xl pr-10"
            @update:model-value="errore = null"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            class="absolute right-1.5 text-(--fg3) hover:bg-transparent hover:text-(--fg) dark:hover:bg-transparent"
            :aria-label="passwordVisibile ? 'Nascondi password' : 'Mostra password'"
            @click="passwordVisibile = !passwordVisibile"
          >
            <EyeOff v-if="passwordVisibile" :size="18" />
            <Eye v-else :size="18" />
          </Button>
        </div>
        <p v-if="errore" id="sessione-scaduta-errore" class="text-xs font-medium text-(--danger)">{{ errore }}</p>
      </form>

      <AlertDialogFooter>
        <Button type="button" variant="neutral" :disabled="inCorso" data-test="sessione-scaduta-esci" @click="esci">
          <LogOut /> Esci
        </Button>
        <Button type="submit" form="form-sessione-scaduta" :disabled="inCorso" data-test="sessione-scaduta-accedi">
          <Loader2 v-if="inCorso" class="animate-spin" />
          <LogIn v-else />
          {{ inCorso ? 'Accesso in corso…' : 'Accedi' }}
        </Button>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
