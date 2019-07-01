// styles are required in polyfills as a workaround
// see : https://github.com/webpack/webpack/issues/1967
import '../../resources/public/styles/admin.scss'
import '../../../../../node_modules/flatpickr/dist/themes/confetti.css'
import '../../../../../node_modules/trumbowyg/dist/ui/trumbowyg.css'
import '../../../../../node_modules/trumbowyg/dist/plugins/colors/ui/trumbowyg.colors.css'
import '../../../../../node_modules/trumbowyg/dist/ui/icons.svg'

import 'core-js/es6'
import 'core-js/es7/reflect'
require('zone.js/dist/zone')

if (process.env.ENV === 'production') {
    // Production
} else {
    // Development and test
    Error['stackTraceLimit'] = Infinity
    require('zone.js/dist/long-stack-trace-zone')
}