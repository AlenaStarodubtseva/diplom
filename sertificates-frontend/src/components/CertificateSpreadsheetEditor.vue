<template>
  <q-dialog v-model="dialogModel" maximized persistent>
    <q-card class="editor-dialog">
      <q-card-section class="row items-center q-gutter-sm editor-toolbar">
        <div>
          <div class="text-h6">Редактор справок для печати</div>
          <div class="text-grey-7 text-body2">
            Предпросмотр XLS-макета
          </div>
        </div>

        <q-space />

        <q-btn
          unelevated
          color="primary"
          icon="table_view"
          label="Открыть XLS"
          @click="openXlsTemplate"
        />

        <q-btn
          unelevated
          color="primary"
          icon="print"
          label="Печать"
          :disable="!!previewError"
          @click="printPreview"
        />

        <q-btn
          outline
          class="campus-accent"
          icon="close"
          label="Закрыть"
          @click="dialogModel = false"
        />
      </q-card-section>

      <q-separator />

      <q-banner
        v-if="previewError"
        rounded
        class="bg-red-1 text-negative q-ma-md"
      >
        {{ previewError }}
      </q-banner>

      <div v-else class="preview-frame-wrap">
        <iframe
          ref="previewFrame"
          class="preview-frame"
          :srcdoc="previewHtml"
          title="Предпросмотр справки"
        ></iframe>
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  certificates: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const previewFrame = ref(null)
const previewHtml = ref('')
const previewError = ref('')

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

watch(
  () => props.modelValue,
  async value => {
    if (value && !previewHtml.value) {
      await loadPreviewHtml()
    }
  }
)

async function loadPreviewHtml() {
  previewError.value = ''

  try {
    const response = await fetch('/templates/certificate-template.files/sheet001.htm')

    if (!response.ok) {
      throw new Error('Файл предпросмотра не найден')
    }

    const buffer = await response.arrayBuffer()

    const decoder = new TextDecoder('windows-1251')
    let html = decoder.decode(buffer)

    html = prepareExcelHtml(html)

    previewHtml.value = html
  } catch (err) {
    console.error(err)
    previewError.value = 'Не удалось загрузить предпросмотр XLS-макета. Проверь файл public/templates/certificate-template.files/sheet001.htm.'
  }
}

function prepareExcelHtml(html) {
  let result = html

  result = result.replace(
    /<!\[if !supportTabStrip\]>[\s\S]*?<!\[endif\]>/gi,
    ''
  )

  result = result.replace(
    /<meta[^>]+charset=windows-1251[^>]*>/i,
    '<meta charset="utf-8">'
  )

  result = result.replace(
    /<head>/i,
    '<head><base href="/templates/certificate-template.files/">'
  )

  result = result.replace(
    /<body([^>]*)>/i,
    '<body$1><div class="preview-page">'
  )

  result = result.replace(
    /<\/body>/i,
    '</div></body>'
  )

  result = result.replace(
    /<\/head>/i,
    `<style>
      body {
        margin: 0;
        background: #f1f1f1;
      }

      .preview-page {
        display: flex;
        justify-content: center;
        padding: 20px;
        box-sizing: border-box;
      }

      table {
        background: white;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
      }
    </style></head>`
  )

  return result
}

function openXlsTemplate() {
  window.open('/templates/certificate-template.xls', '_blank')
}

function printPreview() {
  const frameWindow = previewFrame.value?.contentWindow

  if (frameWindow) {
    frameWindow.focus()
    frameWindow.print()
  }
}
</script>

<style scoped>
.editor-dialog {
  background: #f5f5f5;
}

.editor-toolbar {
  background: #ffffff;
}

.campus-accent {
  color: #7a0019;
  border-color: #7a0019;
}

.preview-frame-wrap {
  height: calc(100vh - 92px);
  background: #eeeeee;
  padding: 12px;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: #ffffff;
}
</style>
