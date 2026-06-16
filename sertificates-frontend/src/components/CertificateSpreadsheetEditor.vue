<template>
  <q-dialog v-model="dialogModel" maximized persistent>
    <q-card class="editor-dialog">
      <q-card-section class="row items-center q-gutter-sm editor-toolbar">
        <div class="text-h6">Редактор справок для печати</div>

        <q-space />

        <q-btn
          unelevated
          color="primary"
          icon="print"
          label="Печать"
          @click="printSheet"
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

      <div id="certificate-luckysheet" class="luckysheet-container"></div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, nextTick, watch } from 'vue'
import 'luckysheet/dist/plugins/css/pluginsCss.css'
import 'luckysheet/dist/plugins/plugins.css'
import 'luckysheet/dist/css/luckysheet.css'
import 'luckysheet/dist/assets/iconfont/iconfont.css'
import luckysheet from 'luckysheet'

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

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

watch(
  () => props.modelValue,
  async value => {
    if (value) {
      await nextTick()
      initSheet()
    }
  }
)

function initSheet() {
  const container = document.getElementById('certificate-luckysheet')
  if (!container) return

  container.innerHTML = ''

  luckysheet.create({
    container: 'certificate-luckysheet',
    lang: 'zh',
    showinfobar: false,
    showsheetbar: false,
    showtoolbar: true,
    showstatisticBar: false,
    allowEdit: true,
    enableAddRow: false,
    enableAddCol: false,
    data: [
      {
        name: 'Справки',
        color: '',
        index: 0,
        status: 1,
        order: 0,
        hide: 0,
        row: Math.max(34, props.certificates.length * 34),
        column: 9,
        defaultRowHeight: 22,
        defaultColWidth: 90,
        celldata: buildCells(props.certificates),
        config: buildConfig(props.certificates)
      }
    ]
  })
}

function buildCells(certificates) {
  const cells = []

  certificates.forEach((certificate, index) => {
    const start = index * 34

    addCell(cells, start + 1, 1, 'МИНИСТЕРСТВО ПРОСВЕЩЕНИЯ\nРОССИЙСКОЙ ФЕДЕРАЦИИ', true, 'center')
    addCell(cells, start + 3, 1, 'федеральное государственное\nбюджетное образовательное\nучреждение высшего\nобразования', true, 'center')
    addCell(cells, start + 7, 1, '«Благовещенский государственный\nпедагогический университет»\n(ФГБОУ ВО «БГПУ»)', true, 'center')
    addCell(cells, start + 12, 1, 'Ленина ул., д. 104, г. Благовещенск\nАмурская область, 675000\nТел./факс (4162) 99-16-26\nE-mail: rektorat@bgpu.ru\nhttp://www.bgpu.ru', false, 'center')
    addCell(cells, start + 17, 1, `На № ${certificate.registrationNumber || '________'} от ${certificate.issueDate || '________'}`, false, 'center')
    addCell(cells, start + 21, 1, 'Лицензия на право осуществления\nобразовательной деятельности\nрегистрационный номер\n№Л035-00115-28/00097102 от 29.02.2016 г.\nпредоставлена бессрочно.', true, 'center')

    addCell(cells, start + 1, 6, 'СПРАВКА', false, 'center')

    addCell(cells, start + 3, 4, 'Выдана')
    addRow(cells, start + 4, 'ФИО:', certificate.studentFullName)
    addRow(cells, start + 5, 'Дата рождения:', certificate.birthDate)
    addRow(cells, start + 6, 'Курс:', certificate.course)
    addRow(cells, start + 7, 'Факультет:', certificate.facultyName)
    addRow(cells, start + 9, 'Направление подготовки:', certificate.direction)
    addRow(cells, start + 11, 'Профиль:', certificate.profile)
    addRow(cells, start + 12, 'Группа:', certificate.groupName)
    addRow(cells, start + 13, 'Форма обучения:', certificate.educationForm)
    addRow(cells, start + 14, 'Основа обучения:', certificate.educationBasis)

    addCell(
      cells,
      start + 15,
      4,
      'Обучается по основной образовательной программе бакалавриата, предусмотренной федеральным государственным образовательным стандартом.',
      false,
      'left'
    )

    addRow(cells, start + 18, 'Начало обучения:', certificate.studyPeriod)
    addRow(cells, start + 21, 'Справка выдана для предъявления:', certificate.purpose)
    addRow(cells, start + 24, 'Основание выдачи справки:', certificate.enrollmentOrder)

    addCell(cells, start + 29, 4, `Декан факультета __________________ ${certificate.deanName || ''}`)
    addCell(cells, start + 31, 4, `Секретарь __________________ ${certificate.secretaryName || ''}`)

    addCell(cells, start + 33, 0, '----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------')
  })

  return cells
}

function addRow(cells, row, label, value) {
  addCell(cells, row, 4, label, true, 'right')
  addCell(cells, row, 5, value || '—', false, 'left')
}

function addCell(cells, row, col, value, bold = false, align = 'left') {
  cells.push({
    r: row,
    c: col,
    v: {
      v: value || '',
      m: value || '',
      ct: { fa: 'General', t: 'g' },
      ff: 'Times New Roman',
      fs: 10,
      bl: bold ? 1 : 0,
      ht: alignCode(align),
      vt: 0,
      tb: 2
    }
  })
}

function alignCode(align) {
  if (align === 'center') return 0
  if (align === 'right') return 2
  return 1
}

function buildConfig(certificates) {
  const merge = {}
  const borderInfo = []

  certificates.forEach((_, index) => {
    const start = index * 34

    addMerge(merge, start + 1, 1, 2, 2)
    addMerge(merge, start + 3, 1, 4, 2)
    addMerge(merge, start + 7, 1, 3, 2)
    addMerge(merge, start + 12, 1, 5, 2)
    addMerge(merge, start + 17, 1, 1, 2)
    addMerge(merge, start + 21, 1, 5, 2)

    addMerge(merge, start + 1, 6, 1, 2)

    for (let r = start + 4; r <= start + 24; r++) {
      addMerge(merge, r, 5, 1, 4)
    }

    addMerge(merge, start + 15, 4, 2, 5)
    addMerge(merge, start + 29, 4, 1, 5)
    addMerge(merge, start + 31, 4, 1, 5)
    addMerge(merge, start + 33, 0, 1, 9)

    borderInfo.push({
      rangeType: 'range',
      value: {
        row: [start, start + 32],
        column: [0, 8]
      },
      borderType: 'border-all',
      style: '1',
      color: '#d9d9d9'
    })

    borderInfo.push({
      rangeType: 'range',
      value: {
        row: [start, start + 32],
        column: [3, 3]
      },
      borderType: 'border-right',
      style: '1',
      color: '#000000'
    })
  })

  return {
    merge,
    borderInfo,
    columnlen: {
      0: 42,
      1: 90,
      2: 90,
      3: 42,
      4: 130,
      5: 95,
      6: 95,
      7: 95,
      8: 95
    },
    rowlen: buildRows(certificates)
  }
}

function addMerge(merge, row, col, rowSpan, colSpan) {
  merge[`${row}_${col}`] = {
    r: row,
    c: col,
    rs: rowSpan,
    cs: colSpan
  }
}

function buildRows(certificates) {
  const rows = {}

  certificates.forEach((_, index) => {
    const start = index * 34

    for (let i = start; i < start + 34; i++) {
      rows[i] = 22
    }

    rows[start + 3] = 54
    rows[start + 7] = 50
    rows[start + 12] = 70
    rows[start + 15] = 48
    rows[start + 21] = 54
  })

  return rows
}

function printSheet() {
  window.print()
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

.luckysheet-container {
  width: 100%;
  height: calc(100vh - 92px);
}

@media print {
  :global(body *) {
    visibility: hidden;
  }

  :global(#certificate-luckysheet),
  :global(#certificate-luckysheet *) {
    visibility: visible;
  }

  :global(#certificate-luckysheet) {
    position: absolute;
    left: 0;
    top: 0;
    width: 297mm;
    height: auto;
  }

  :global(.editor-toolbar),
  :global(.luckysheet-toolbar),
  :global(.luckysheet-wa-editor),
  :global(.luckysheet-sheets) {
    display: none !important;
  }

  @page {
    size: A4 landscape;
    margin: 0;
  }
}
</style>
