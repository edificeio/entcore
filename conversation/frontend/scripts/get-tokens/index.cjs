const fs = require('fs');
const puppeteer = require('puppeteer');
const inquirer = require('inquirer');
const configPath = `${__dirname}/config.json`;

const isAutoMode = process.argv.includes('--auto');
let browser;

process.on('SIGINT', async () => {
  console.log('🚨 Signal SIGINT détecté. Fermeture de Puppeteer...');
  if (browser) {
    await browser.close();
    console.log('✅ Puppeteer fermé.');
  }
  process.exit(0);
});

// Create config.json if needed
if (!fs.existsSync(configPath)) {
  console.log('⚠️  Aucun fichier config.json trouvé. Création en cours...');
  fs.writeFileSync(
    configPath,
    JSON.stringify({ recettes: [], profils: [] }, null, 2),
  );
  console.log('✅ Fichier config.json créé.');
}

let config = require(configPath);

async function initialiserConfig() {
  if (!config.recettes || config.recettes.length === 0) {
    if (isAutoMode) {
      console.error(
        '❌ Aucune recette disponible en mode auto. Ajoutez-en une dans config.json.',
      );
      process.exit(1);
    }
    const recetteAnswer = await inquirer.prompt([
      {
        type: 'input',
        name: 'recette',
        message: "🌍 Entrez l'URL de la recette :",
      },
    ]);
    config.recettes = [recetteAnswer.recette];
  }
  fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
}

async function choisirProfil() {
  if (config.profils.length === 0 || isAutoMode) {
    if (config.profils.length === 0) {
      const newProfil = await inquirer.prompt([
        { type: 'input', name: 'login', message: '🆕 Entrez le login :' },
        {
          type: 'password',
          name: 'password',
          message: '🔒 Entrez le mot de passe :',
        },
      ]);
      return newProfil;
    }
    return config.profils[0]; // Auto-mode
  }

  const profiles = config.profils.map((profil, index) => ({
    name: profil.login,
    value: index,
  }));
  profiles.push({ name: '🔑 Ajouter un nouveau profil', value: 'new' });

  const profilAnswer = await inquirer.prompt([
    {
      type: 'list',
      name: 'profilIndex',
      message: '📌 Sélectionnez un profil :',
      choices: profiles,
    },
  ]);

  if (profilAnswer.profilIndex === 'new') {
    return await inquirer.prompt([
      { type: 'input', name: 'login', message: '🆕 Entrez le login :' },
      {
        type: 'password',
        name: 'password',
        message: '🔒 Entrez le mot de passe :',
      },
    ]);
  }

  return config.profils[profilAnswer.profilIndex];
}

(async () => {
  try {
    await initialiserConfig();
    config = require(configPath);

    let selectedRecette =
      config.recettes.length === 1 || isAutoMode
        ? config.recettes[0]
        : (
            await inquirer.prompt([
              {
                type: 'list',
                name: 'recette',
                message: '📌 Sélectionnez une recette :',
                choices: config.recettes,
              },
            ])
          ).recette;

    console.log(`✅ Recette sélectionnée : ${selectedRecette}`);

    let selectedProfil = await choisirProfil();
    console.log(
      `🌍 Connexion en tant que ${selectedProfil.login} sur ${selectedRecette}`,
    );

    browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();

    await page.goto(selectedRecette, { waitUntil: 'networkidle2' });
    await page.type('#email', selectedProfil.login);
    await page.type('#password', selectedProfil.password);
    await page.click('button.flex-magnet-bottom-right');
    await page.waitForNavigation().catch(() => {});

    const cookies = await page.cookies();
    const xsrfToken = cookies.find((c) => c.name === 'XSRF-TOKEN')?.value || '';
    const sessionId =
      cookies.find((c) => c.name === 'oneSessionId')?.value || '';

    if (!xsrfToken || !sessionId) {
      console.error(
        '❌ Échec de la connexion. Vérifiez les identifiants et réessayez.',
      );
      await browser.close();
      process.exit(1);
    }

    console.log('🔑 Connexion réussie, récupération des cookies...');
    const now = new Date();
    const timestamp = now.toLocaleString('fr-FR', { timeZone: 'Europe/Paris' });
    const envContent = `# Connected as: ${selectedProfil.login}\n# Date: ${timestamp}\n\nVITE_XSRF_TOKEN=${xsrfToken}\nVITE_ONE_SESSION_ID=${sessionId}\nVITE_RECETTE=${selectedRecette}\n`;
    fs.writeFileSync('.env', envContent);
    console.log('✅ Cookies enregistrés dans .env');

    if (
      !config.profils.some((profil) => profil.login === selectedProfil.login) &&
      !isAutoMode
    ) {
      console.log('🔑 Nouveau profil ajouté à la configuration.');
      config.profils.push(selectedProfil);
      fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
    }
  } catch (error) {
    console.error('❌ Une erreur est survenue lors de la connexion:', error);
  } finally {
    if (browser) {
      await browser.close();
      console.log('✅ Navigateur Puppeteer fermé.');
    }
    process.exit(0);
  }
})();
