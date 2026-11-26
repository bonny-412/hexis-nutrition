<script setup lang="ts">
// Cornice comune di login, password dimenticata e nuova password: pannello scuro con il marchio
// da 768 px in su, logo compatto sopra il modulo da mobile. Il contenuto (titolo e modulo) va nello slot.
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 bg-(--bg) md:grid-cols-12">
    <div class="relative hidden flex-col justify-between overflow-hidden bg-(--side) p-12 text-(--on-hero) md:col-span-6 md:flex lg:col-span-7 xl:p-16">
      <div class="pointer-events-none absolute -left-20 -top-20 h-96 w-96 rounded-full bg-(--sage)/20 blur-3xl" />
      <div class="pointer-events-none absolute -bottom-32 -right-32 h-120 w-120 rounded-full bg-(--sage)/10 blur-3xl" />

      <div class="relative z-10 flex items-center gap-3">
        <div class="flex size-11 items-center justify-center">
          <img src="@/assets/hexis-logo.svg" alt="" class="h-full w-full object-contain" />
        </div>
        <span class="font-heading text-xl font-semibold tracking-tight text-(--on-hero)">Hexis Nutrition</span>
      </div>

      <div class="relative z-10 my-auto max-w-lg py-12">
        <!-- Motto del marchio, non un titolo: il titolo della pagina è l'h1 del modulo. -->
        <p class="font-heading text-4xl font-normal italic leading-tight text-(--on-hero) xl:text-5xl">
          Il tuo studio nutrizionale, in un unico posto.
        </p>
        <p class="mt-4 text-base leading-relaxed text-(--side-fg)">
          Pazienti, visite, piani alimentari e agenda: dalla prima visita al controllo successivo.
        </p>
      </div>

      <div class="relative z-10 flex items-center justify-between text-xs text-(--side-fg)">
        <!-- "Privacy Policy" tolta finché non esiste la pagina: era testo che sembrava un link. -->
        <span>© 2026 Hexis Nutrition</span>
      </div>
    </div>

    <div class="flex flex-col items-center justify-center p-6 md:col-span-6 md:p-12 lg:col-span-5">
      <div class="w-full max-w-sm">
        <div class="mb-8 flex items-center gap-3 md:hidden">
          <img src="@/assets/hexis-logo.svg" alt="" class="size-10 rounded-xl bg-(--side) p-1.5" />
          <span class="font-heading text-xl font-semibold text-(--fg)">Hexis Nutrition</span>
        </div>
        <slot />
      </div>
    </div>
  </div>
</template>
