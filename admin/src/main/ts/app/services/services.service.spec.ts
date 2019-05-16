import { ServicesService } from "./services.service";
import { TestBed } from "@angular/core/testing";
import { ConnectorModel } from "../core/store";
import { HttpTestingController, HttpClientTestingModule } from "@angular/common/http/testing";
import { Profile } from "./shared/services-types";

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

    describe('createConnector', () => {
        it('should call POST /appregistry/application/external?structureId=${structureId} with given structureId and given connector in request body', () => {
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
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || ''
            });
        });
    });

    describe('saveConnector', () => {
        it('should call PUT /appregistry/application/conf/${connector.id}?structureId=${structureId}with given structureId and given connector in request body', () => {
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
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || ''
            });
        });
    });

    describe('deleteConnector', () => {
        it('should call DELETE /appregistry/application/external/${connector.id} with given connector', () => {
            servicesService.deleteConnector(connector).subscribe();
            const request = httpTestingController.expectOne(
                `/appregistry/application/external/${connector.id}`);
            expect(request.request.method).toBe('DELETE');
        });
    });

    describe('toggleLockConnector', () => {
        it('should call PUT /appregistry/application/external/${connector.id}/lock with given connectorId', () => {
            servicesService.toggleLockConnector(connector).subscribe();
            const request = httpTestingController.expectOne(
                `/appregistry/application/external/${connector.id}/lock`);
            expect(request.request.method).toBe('PUT');
        });
    });

    describe('gtCasTypes', () => {
        it('should call GET /appregistry/cas-types', () => {
            servicesService.getCasTypes().subscribe();
            const request = httpTestingController.expectOne(`/appregistry/cas-types`);
            expect(request.request.method).toBe('GET');
        });
    });

    describe('massAssignConnector', () => {
        it('should call PUT /appregistry/application/external/${connector.id}/authorize?profile=Teacher with given connectorId and given profiles', () => {
            const profiles: Profile[] = ['Teacher'];
            servicesService.massAssignConnector(connector, profiles).subscribe();
    
            const expectedProfilesParams: string = '?profile=Teacher';
            const request = httpTestingController.expectOne(
                `/appregistry/application/external/${connector.id}/authorize${expectedProfilesParams}`);
            expect(request.request.method).toBe('PUT');
        });
    });

    describe('massUnassignConnector', () => {
        it('should call DELETE /appregistry/application/external/${connector.id}/authorize?profile=Teacher with given connectorId and given profiles', () => {
            const profiles: Profile[] = ['Teacher'];
            servicesService.massUnassignConnector(connector, profiles).subscribe();
    
            const expectedProfilesParams: string = '?profile=Teacher';
            const request = httpTestingController.expectOne(
                `/appregistry/application/external/${connector.id}/authorize${expectedProfilesParams}`);
            expect(request.request.method).toBe('DELETE');
        });
    });

    describe('getExportConnectorUrl', () => {
        it('should return /directory/export/users?format=csv&structureId=structure1&type=Gepi given format.format=csv, format.value=Gepi, profile=all and structureId=structure1', () => {
            const res: string = servicesService.getExportConnectorUrl({
                format: 'csv', 
                value: 'Gepi', 
                profiles: [], 
                label: ''
            }, 'Teacher', 'structure1');
            expect(res).toBe('/directory/export/users?format=csv&structureId=structure1&type=Gepi&profile=Teacher');
        });

        it('should return /directory/export/users?format=csv&structureId=structure1&type=Gepi given format.format=csv, format.value=Gepi, profile=all and structureId structure1', () => {
            const res: string = servicesService.getExportConnectorUrl({
                format: 'csv', 
                value: 'Gepi', 
                profiles: [], 
                label: ''
            }, 'all', 'structure1');
            expect(res).toBe('/directory/export/users?format=csv&structureId=structure1&type=Gepi');
        });
    });
})