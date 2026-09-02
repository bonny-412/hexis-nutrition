<script setup lang="ts">
import { Toaster } from '@/components/ui/sonner'
</script>

<template>
  <router-view />
  <Teleport to="body">
    <Toaster rich-colors close-button close-button-position="top-right" position="top-right" :duration="5000" />
  </Teleport>
</template>
