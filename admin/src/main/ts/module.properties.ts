import * as components from './components'
import * as directives from './directives'
import * as routing from './routing'
import * as services from './services'

export let declarations = []
export let providers = []

// Component and directives declarations //

for(let component in components) {
    declarations.push(components[component])
}
for(let directive in directives) {
    declarations.push(directives[directive])
}

// Service providers //

for(let routingService in routing) {
    if(routingService !== 'routes' && routingService !== 'routing') {
        providers.push(routing[routingService])
    }
}
for(let service in services) {
    providers.push(services[service])
}