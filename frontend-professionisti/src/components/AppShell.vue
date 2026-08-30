<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'

const sidebarAperta = ref(false)
</script>

<template>
  <div class="flex min-h-screen overflow-x-hidden bg-[var(--bg)]">
    <AppSidebar class="m-[14px_0_14px_14px] hidden rounded-[18px] border border-[var(--side-bd)] shadow-[var(--shadow)] lg:flex" />

    <Sheet v-model:open="sidebarAperta">
      <SheetContent
        side="left"
        class="data-[side=left]:w-[246px] data-[side=left]:max-w-none data-[side=left]:border-r-0 bg-transparent p-0 shadow-none"
        :show-close-button="false"
      >
        <SheetTitle class="sr-only">Menu di navigazione</SheetTitle>
        <AppSidebar class="h-full" />
      </SheetContent>
    </Sheet>

    <div class="flex flex-1 flex-col">
      <AppHeader @apri-menu="sidebarAperta = true" />
      <main class="flex-1 p-8">
        <slot />
      </main>
    </div>
  </div>
</template>
