import { ServicesService } from "./services.service";
import { TestBed } from "@angular/core/testing";
import { ConnectorModel } from "../core/store";
import { HttpTestingController, HttpClientTestingModule } from "@angular/common/http/testing";

describe('ServicesService', () => {
    let servicesService: ServicesService;
    let httpTestingController: HttpTestingController;
    let connector: ConnectorModel;
    let structureId: string;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ServicesService
            ],
            imports: [
                HttpClientTestingModule
            ]
        });
        servicesService = TestBed.get(ServicesService);
        httpTestingController = TestBed.get(HttpTestingController);
        connector = new ConnectorModel();
        connector.name = 'connector1';
        connector.displayName = 'connector1';
        connector.url = '/connector1';
        structureId = 'structure1';
    });

    it('should create connector', () => {
        servicesService.createConnector(connector, structureId).subscribe();
        const request = httpTestingController.expectOne(
            `/appregistry/application/external?structureId=${structureId}`);
        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual({
            name: connector.name,
            displayName: connector.displayName,
            icon: connector.icon || '',
            address: connector.url,
            target: connector.target || '',
            inherits: connector.inherits || false,
            appLocked: connector.locked || false,
            casType: connector.casTypeId || '',
            pattern: connector.casPattern || '',
            scope: connector.oauthScope || '',
            secret: connector.oauthSecret || '',
            grantType: connector.oauthGrantType || ''
        });
    });

    it('should save connector', () => {
        servicesService.saveConnector(connector, structureId).subscribe();
        const request = httpTestingController.expectOne(
            `/appregistry/application/conf/${connector.id}?structureId=${structureId}`);
        expect(request.request.method).toBe('PUT');
        expect(request.request.body).toEqual({
            name: connector.name,
            displayName: connector.displayName,
            icon: connector.icon || '',
            address: connector.url,
            target: connector.target || '',
            inherits: connector.inherits || false,
            appLocked: connector.locked || false,
            casType: connector.casTypeId || '',
            pattern: connector.casPattern || '',
            scope: connector.oauthScope || '',
            secret: connector.oauthSecret || '',
            grantType: connector.oauthGrantType || ''
        });
    });

    it('should delete connector', () => {
        servicesService.deleteConnector(connector).subscribe();
        const request = httpTestingController.expectOne(
            `/appregistry/application/external/${connector.id}`);
        expect(request.request.method).toBe('DELETE');
    });

    it('should toggle connector lock', () => {
        servicesService.toggleLockConnector(connector).subscribe();
        const request = httpTestingController.expectOne(
            `/appregistry/application/external/${connector.id}/lock`);
        expect(request.request.method).toBe('PUT');
    });

    it('should get CAS types', () => {
        servicesService.getCasTypes().subscribe();
        const request = httpTestingController.expectOne(`/appregistry/cas-types`);
        expect(request.request.method).toBe('GET');
    });
})