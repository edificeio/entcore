import { ServicesService } from "./services.service";
import { TestBed } from "@angular/core/testing";
import { ConnectorModel } from "../core/store";
import { HttpTestingController, HttpClientTestingModule } from "@angular/common/http/testing";

describe('ServicesService', () => {
    let servicesService: ServicesService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ServicesService
            ],
            imports: [
                HttpClientTestingModule
            ]
        });
        servicesService =TestBed.get(ServicesService);
        httpTestingController = TestBed.get(HttpTestingController);
    });

    it('should create connector', () => {
        const connector = new ConnectorModel();
        connector.name = 'connector1';
        connector.displayName = 'connector1';
        connector.url = '/connector1';
        const structureId = 'structure1';

        servicesService.createConnector(connector, structureId).subscribe();
        const createConnectorRequest = httpTestingController.expectOne(
            `/appregistry/application/external?structureId=${structureId}`);
        expect(createConnectorRequest.request.method).toBe('POST');
    });
})