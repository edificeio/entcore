match (a: Application) set a.appType="END_USER";
match (a: Application) where a.prefix IN ['/auth', '/bookmark', '/cas', '/communication', '/directory', '/eliot', '/rss', '/sso', '/searchengine', '/timeline', '/xiti'] set a.appType='SYSTEM';
match (a: Application) where a.name IN ['AppRegistry', 'app-e', 'Portal', 'ViescolaireChapeau'] set a.appType='SYSTEM';
match (a: Application) where a.prefix IN ['/bookmark', '/rss', '/cursus', '/maxicours'] set a.appType='WIDGET';