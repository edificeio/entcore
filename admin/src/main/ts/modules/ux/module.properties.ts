import * as components from './components'
import * as directives from './directives'

export let declarations = []
export let providers = []

// Component and directives declarations //

for(let component in components) {
    declarations.push(components[component])
}
for(let directive in directives) {
    declarations.push(directives[directive])
}