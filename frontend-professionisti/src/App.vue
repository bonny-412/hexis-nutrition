<script setup lang="ts">
import { onMounted, provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { LoaderCircle } from '@lucide/vue'
import { Toaster } from '@/components/ui/sonner'
import { segnalaErroreConnessione } from '@/composables/useErroreConnessione'
import ErroreConnessioneView from '@/views/ErroreConnessioneView.vue'

const props = defineProps<{ ripristinaSessione?: () => Promise<boolean> }>()
const route = useRoute()
const avvio = ref(!!props.ripristinaSessione)
const riprovaInCorso = ref(false)
const erroreConnessione = ref(false)

provide(segnalaErroreConnessione, () => { erroreConnessione.value = true })

watch(() => route.fullPath, () => { erroreConnessione.value = false })

async function riprova() {
  if (riprovaInCorso.value) return
  riprovaInCorso.value = true
  try {
    const sessioneRipristinata = await props.ripristinaSessione?.()
    erroreConnessione.value = route.meta.requiresAuth === true && sessioneRipristinata === false
  } finally {
    riprovaInCorso.value = false
    avvio.value = false
  }
}

onMounted(() => {
  if (avvio.value) void riprova()
})
</script>

<template>
  <ErroreConnessioneView v-if="erroreConnessione" :in-corso="riprovaInCorso" @riprova="riprova" />
  <router-view v-else-if="!avvio" />
  <main v-else class="grid min-h-dvh place-items-center bg-background text-primary" role="status">
    <LoaderCircle aria-hidden="true" class="size-7 motion-safe:animate-spin" />
    <span class="sr-only">Caricamento dell’area professionisti</span>
  </main>
  <Teleport to="body">
    <Toaster rich-colors close-button close-button-position="top-right" position="top-right" :duration="5000" />
  </Teleport>
</template>
