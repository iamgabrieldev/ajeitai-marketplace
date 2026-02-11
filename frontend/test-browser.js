const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
  const consoleErrors = [];
  const networkErrors = [];

  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  // Registrar listeners ANTES da navegação
  // Capturar erros de console
  page.on('console', msg => {
    if (msg.type() === 'error') {
      const error = {
        message: msg.text(),
        type: msg.type(),
        location: msg.location()
      };
      consoleErrors.push(error);
      console.log('Console Error:', error);
    }
  });

  page.on('pageerror', error => {
    const errorInfo = {
      message: error.message,
      type: 'pageerror',
      stack: error.stack
    };
    consoleErrors.push(errorInfo);
    console.log('Page Error:', errorInfo);
  });

  // Capturar erros de rede
  page.on('response', async response => {
    const status = response.status();
    if (status >= 400) {
      let bodyText = '';
      try {
        bodyText = await response.text();
        // Truncar se muito longo
        if (bodyText.length > 500) {
          bodyText = bodyText.substring(0, 500) + '...';
        }
      } catch (e) {
        bodyText = response.statusText();
      }

      const networkError = {
        url: response.url(),
        method: response.request().method(),
        status: status,
        statusText: response.statusText(),
        body: bodyText
      };
      networkErrors.push(networkError);
      console.log('Network Error:', networkError);
    }
  });

  try {
    console.log('Navegando para http://localhost:3000...');
    await page.goto('http://localhost:3000', { waitUntil: 'domcontentloaded', timeout: 30000 });
    
    // Aguardar um momento para a página carregar
    await page.waitForTimeout(2000);

    const currentUrl = page.url();
    console.log('URL atual:', currentUrl);

    // Verificar se há tela de login
    const hasLoginForm = await page.evaluate(() => {
      // Procurar por campos de login típicos
      const usernameFields = document.querySelectorAll('input[type="text"], input[type="email"], input[name*="user"], input[name*="email"], input[id*="user"], input[id*="email"], input[name*="username"]');
      const passwordFields = document.querySelectorAll('input[type="password"]');
      const submitButtons = document.querySelectorAll('button[type="submit"], input[type="submit"], button:has-text("Entrar"), button:has-text("Login"), button:has-text("Sign in")');
      
      return usernameFields.length > 0 && passwordFields.length > 0;
    });

    const isKeycloakLogin = currentUrl.includes('realms') || currentUrl.includes('protocol/openid-connect') || currentUrl.includes('auth');

    console.log('Tela de login detectada?', hasLoginForm || isKeycloakLogin);

    if (hasLoginForm || isKeycloakLogin) {
      console.log('Detectada tela de login. Realizando login...');
      
      // Tentar encontrar campo de usuário
      const usernameSelector = await page.evaluate(() => {
        const selectors = [
          'input[name="username"]',
          'input[id="username"]',
          'input[name="email"]',
          'input[id="email"]',
          'input[type="email"]',
          'input[name*="user"]',
          'input[id*="user"]'
        ];
        
        for (const selector of selectors) {
          const elem = document.querySelector(selector);
          if (elem) return selector;
        }
        
        // Fallback: primeiro input de texto
        const textInputs = document.querySelectorAll('input[type="text"], input[type="email"]');
        if (textInputs.length > 0) {
          return 'input[type="text"], input[type="email"]';
        }
        return null;
      });

      if (usernameSelector) {
        console.log('Preenchendo campo de usuário...');
        await page.fill(usernameSelector, 'usuario@email.com');
      }

      // Campo de senha
      console.log('Preenchendo campo de senha...');
      await page.fill('input[type="password"]', '123');

      // Aguardar um momento
      await page.waitForTimeout(500);

      // Clicar no botão de login
      const buttonSelector = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button, input[type="submit"]'));
        
        for (const button of buttons) {
          const text = button.textContent || button.value || '';
          if (text.match(/entrar|login|sign in|log in|acessar/i) || button.type === 'submit') {
            // Retornar um seletor único
            if (button.id) return `#${button.id}`;
            if (button.name) return `[name="${button.name}"]`;
            if (button.type === 'submit') return 'button[type="submit"], input[type="submit"]';
            return null;
          }
        }
        return 'button[type="submit"], input[type="submit"]';
      });

      console.log('Clicando no botão de login...');
      await page.click(buttonSelector || 'button[type="submit"]');

      // Aguardar navegação para /cliente/home
      console.log('Aguardando navegação para /cliente/home...');
      try {
        await page.waitForURL('**/cliente/home', { timeout: 30000 });
      } catch (e) {
        console.log('Timeout aguardando /cliente/home, verificando URL atual...');
      }
      
      await page.waitForTimeout(3000);
    }

    // Verificar se estamos em /cliente/home
    const finalUrl = page.url();
    console.log('URL final:', finalUrl);

    if (finalUrl.includes('/cliente/home')) {
      console.log('Página /cliente/home carregada. Aguardando recursos...');
      
      // Aguardar network idle
      try {
        await page.waitForLoadState('networkidle', { timeout: 10000 });
      } catch (e) {
        console.log('Timeout em networkidle, continuando...');
      }

      await page.waitForTimeout(2000);

      // Fazer screenshot
      console.log('Capturando screenshot...');
      await page.screenshot({ 
        path: 'cliente-home.png', 
        fullPage: true 
      });
      console.log('Screenshot salvo como cliente-home.png');
    } else {
      console.log('Navegação não chegou a /cliente/home. URL atual:', finalUrl);
      // Mesmo assim, capturar screenshot
      await page.screenshot({ 
        path: 'cliente-home.png', 
        fullPage: true 
      });
    }

  } catch (error) {
    console.error('Erro durante navegação:', error.message);
    consoleErrors.push({
      message: error.message,
      type: 'navigation-error',
      stack: error.stack
    });
  } finally {
    await browser.close();

    // Salvar resultados em arquivo JSON
    const results = {
      consoleErrors: consoleErrors,
      networkErrors: networkErrors,
      timestamp: new Date().toISOString()
    };

    fs.writeFileSync('test-results.json', JSON.stringify(results, null, 2));
    console.log('\n=== RESULTADOS DO TESTE ===');
    console.log('\nErros de Console:', consoleErrors.length);
    consoleErrors.forEach((err, idx) => {
      console.log(`\n${idx + 1}.`, err);
    });
    
    console.log('\n\nErros de Rede:', networkErrors.length);
    networkErrors.forEach((err, idx) => {
      console.log(`\n${idx + 1}.`, err);
    });

    console.log('\n\nResultados salvos em test-results.json');
  }
})();
