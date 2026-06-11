<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  modelValue: string
  options: { code: string; name: string }[]
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:code': [code: string]
  change: []
}>()

const inputVal = ref(props.modelValue)
const showDropdown = ref(false)
const highlightIdx = ref(-1)
const wrapperRef = ref<HTMLElement | null>(null)

const filtered = computed(() => {
  const q = inputVal.value.trim()
  if (!q) return props.options.slice(0, 10)
  const lower = q.toLowerCase()
  return props.options
    .filter((o) => o.name.includes(q) || o.name.toLowerCase().includes(lower))
    .slice(0, 15)
})

watch(
  () => props.modelValue,
  (v) => {
    inputVal.value = v
  },
)

function select(opt: { code: string; name: string }) {
  inputVal.value = opt.name
  emit('update:modelValue', opt.name)
  emit('update:code', opt.code)
  emit('change')
  showDropdown.value = false
  highlightIdx.value = -1
}

function onFocus() {
  showDropdown.value = true
  highlightIdx.value = -1
}

function onInput(e: Event) {
  inputVal.value = (e.target as HTMLInputElement).value
  emit('update:modelValue', inputVal.value)
  showDropdown.value = true
  highlightIdx.value = -1
}

function onBlur() {
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

function onKeydown(e: KeyboardEvent) {
  if (!showDropdown.value || !filtered.value.length) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    highlightIdx.value = Math.min(highlightIdx.value + 1, filtered.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    highlightIdx.value = Math.max(highlightIdx.value - 1, 0)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const highlighted = filtered.value[highlightIdx.value]
    if (highlightIdx.value >= 0 && highlighted) {
      select(highlighted)
    }
  } else if (e.key === 'Escape') {
    showDropdown.value = false
    highlightIdx.value = -1
  }
}

function onClickOutside(e: MouseEvent) {
  if (wrapperRef.value && !wrapperRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}
onMounted(() => document.addEventListener('click', onClickOutside))
onUnmounted(() => document.removeEventListener('click', onClickOutside))
</script>

<template>
  <div ref="wrapperRef" class="autocomplete">
    <input
      :value="inputVal"
      :placeholder="placeholder"
      :disabled="disabled"
      type="text"
      autocomplete="off"
      class="ac-input"
      @focus="onFocus"
      @input="onInput"
      @keydown="onKeydown"
      @blur="onBlur"
    />
    <ul v-if="showDropdown && filtered.length" class="ac-drop">
      <li
        v-for="(opt, idx) in filtered"
        :key="opt.code"
        :class="{ hl: idx === highlightIdx }"
        @mousedown.prevent="select(opt)"
      >
        {{ opt.name }}
      </li>
    </ul>
  </div>
</template>

<style scoped>
.autocomplete {
  position: relative;
  width: 100%;
}
.ac-input {
  width: 100%;
  padding: 0.5rem 0.7rem;
  border: 1px solid var(--line-strong);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  outline: none;
  background: var(--surface);
  box-sizing: border-box;
  line-height: 1.5;
}
.ac-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-ring);
}
.ac-drop {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 220px;
  overflow-y: auto;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 30;
  padding: 4px 0;
  margin: 0;
  list-style: none;
}
.ac-drop li {
  padding: 0.5rem 0.8rem;
  cursor: pointer;
  font-size: 0.9rem;
  color: var(--ink);
  transition: background 0.08s;
}
.ac-drop li:hover,
.ac-drop li.hl {
  background: var(--primary-soft);
  color: var(--primary-hover);
}
</style>
