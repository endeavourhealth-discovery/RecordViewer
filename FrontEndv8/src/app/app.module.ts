import { NgModule, DoBootstrap, ApplicationRef } from '@angular/core';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import {AppMenuService} from './app-menu.service';
import {RouterModule} from '@angular/router';
import {CareRecordModule} from './carerecord/carerecord.module';
import {HttpClientModule} from '@angular/common/http';
import {AbstractMenuProvider, LayoutComponent, LayoutModule, LoggerModule, SecurityModule, UserManagerModule} from 'dds-angular8';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import {MAT_DATE_LOCALE} from "@angular/material/core";

const keycloakService = new KeycloakService();

@NgModule({
  imports: [
    KeycloakAngularModule,
    HttpClientModule,
    LayoutModule,
    SecurityModule,
    LoggerModule,
    UserManagerModule,
    CareRecordModule,
    NgxChartsModule,
    RouterModule.forRoot(AppMenuService.getRoutes(), {useHash: true}),
  ],
  providers: [
    AppMenuService,
    { provide: AbstractMenuProvider, useExisting : AppMenuService },
    { provide: KeycloakService, useValue: keycloakService },
    { provide: MAT_DATE_LOCALE, useValue: 'en-GB' }
  ]
})
export class AppModule implements DoBootstrap {
  ngDoBootstrap(appRef: ApplicationRef) {
    keycloakService
      .init({config: 'public/wellknown/authconfigraw', initOptions: {onLoad: 'login-required', 'checkLoginIframe': false}})
      .then((authenticated) => {
        if (authenticated)
          appRef.bootstrap(LayoutComponent);
      })
      .catch(error => console.error('[ngDoBootstrap] init Keycloak failed', error));
  }
}
