<#--
  Design System: Marketplace UI
  Layout: Split Screen (PWA Friendly)
  Framework: Tailwind CSS (Injected)
-->
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>${msg("loginTitle",(realm.displayName!'Marketplace'))}</title>

    <!-- Fonte Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

    <!-- Tailwind CSS (CDN para prototipagem rápida no Keycloak) -->
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: {
                            DEFAULT: '#FF6D00', // Laranja Principal
                            hover: '#E65100',
                            light: '#FFF3E0'
                        },
                        text: {
                            primary: '#1A2B40', // Azul Marinho Profundo
                            secondary: '#64748B', // Cool Grey
                        },
                        surface: '#F8FAFC',
                        error: '#D32F2F'
                    },
                    fontFamily: {
                        sans: ['Inter', 'sans-serif'],
                    }
                }
            }
        }
    </script>

    <style>
        /* Ajustes específicos para garantir altura total */
        html, body { height: 100%; margin: 0; }
        .login-bg {
            background-image: url('https://images.unsplash.com/photo-1621905251189-08b45d6a269e?q=80&w=2069&auto=format&fit=crop');
            background-size: cover;
            background-position: center;
        }
    </style>
</head>

<body class="bg-surface font-sans h-full w-full flex overflow-hidden">

<!-- LADO ESQUERDO: Imagem e Branding (Escondido no Mobile) -->
<div class="hidden lg:flex w-1/2 relative bg-gray-900 h-full login-bg">
    <!-- Overlay Laranja (Multiply Effect) -->
    <div class="absolute inset-0 bg-brand-DEFAULT mix-blend-multiply opacity-80"></div>
    <div class="absolute inset-0 bg-gradient-to-t from-text-primary/90 to-transparent"></div>

    <!-- Conteúdo Institucional -->
    <div class="relative z-10 p-16 flex flex-col justify-end h-full text-white">
        <h2 class="text-4xl font-bold mb-4 leading-tight">Conectando quem precisa<br>a quem sabe fazer.</h2>
        <div class="flex items-center gap-4 mt-4 opacity-90">
            <div class="flex gap-1 text-yellow-400">
                <!-- Ícones de Estrela (SVG Inline para evitar dependências) -->
                <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            </div>
            <span class="text-sm font-medium">Plataforma verificada</span>
        </div>
    </div>
</div>

<!-- LADO DIREITO: Formulário de Login -->
<div class="w-full lg:w-1/2 h-full flex flex-col bg-white overflow-y-auto">
    <div class="flex-1 flex flex-col justify-center px-8 sm:px-16 md:px-24 max-w-2xl mx-auto w-full py-12">

        <!-- Keycloak Header/Logo area -->
        <div class="mb-10">
            <!-- Icone/Logo -->
            <h1 class="text-3xl font-bold text-text-primary mb-2">${msg("loginTitleHtml", (realm.displayName!'Marketplace'))?no_esc}</h1>
            <p class="text-text-secondary">Acesse para gerir serviços ou encontrar profissionais.</p>
        </div>

        <!-- FEEDBACK / ALERTAS DE ERRO DO KEYCLOAK -->
        <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="mb-6 p-4 rounded-lg flex items-start gap-3
                    <#if message.type = 'success'>bg-green-50 text-green-800 border border-green-200
                    <#elseif message.type = 'warning'>bg-yellow-50 text-yellow-800 border border-yellow-200
                    <#elseif message.type = 'error'>bg-red-50 text-error border border-red-200
                    <#else>bg-blue-50 text-blue-800 border border-blue-200</#if>">

                    <span class="mt-0.5">
                        <#if message.type = 'success'>
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        <#elseif message.type = 'error'>
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <#else>
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        </#if>
                    </span>
                <span class="text-sm font-medium">${kcSanitize(message.summary)?no_esc}</span>
            </div>
        </#if>

        <!-- FORMULÁRIO -->
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" class="space-y-6">

                <!-- Username / Email -->
                <div>
                    <label for="username" class="block text-sm font-medium text-text-primary mb-2">
                        <#if !realm.loginWithEmailAllowed>${msg("Email")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                    </label>
                    <input tabindex="1" id="username" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="username"
                           class="w-full h-14 px-4 bg-white border border-slate-200 rounded-lg text-text-primary focus:outline-none focus:border-brand-DEFAULT focus:ring-1 focus:ring-brand-DEFAULT transition-all placeholder-slate-400"
                           placeholder="${msg('Fulano@email.com')}" />
                </div>

                <!-- Senha -->
                <div>
                    <div class="flex items-center justify-between mb-2">
                        <label for="password" class="block text-sm font-medium text-text-primary">${msg("Senha")}</label>

                        <#if realm.resetPasswordAllowed>
                            <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="text-sm font-medium text-brand-DEFAULT hover:text-brand-hover">
                                ${msg("Esqueceu a senha?")}
                            </a>
                        </#if>
                    </div>
                    <input tabindex="2" id="password" name="password" type="password" autocomplete="current-password"
                           class="w-full h-14 px-4 bg-white border border-slate-200 rounded-lg text-text-primary focus:outline-none focus:border-brand-DEFAULT focus:ring-1 focus:ring-brand-DEFAULT transition-all placeholder-slate-400" />
                </div>

                <!-- Remember Me -->
                <#if realm.rememberMe && !login.rememberMe??>
                    <div class="flex items-center">
                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                               class="h-4 w-4 text-brand-DEFAULT focus:ring-brand-DEFAULT border-gray-300 rounded">
                        <label for="rememberMe" class="ml-2 block text-sm text-text-secondary">
                            ${msg("rememberMe")}
                        </label>
                    </div>
                </#if>

                <!-- Botão Submit -->
                <div id="kc-form-buttons">
                    <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    <input tabindex="4"
                           class="w-full h-14 bg-brand-DEFAULT  bg-brand-hover text-white font-bold rounded-lg shadow-md hover:shadow-lg transition-all cursor-pointer uppercase tracking-wide text-sm"
                           name="login" id="kc-login" type="submit" value="${msg('Entrar')}"/>
                </div>
            </form>
        </#if>

        <!-- Social Providers (Se existirem) -->
        <#if realm.password && social.providers??>
            <div class="mt-8">
                <div class="relative">
                    <div class="absolute inset-0 flex items-center"><div class="w-full border-t border-slate-200"></div></div>
                    <div class="relative flex justify-center text-sm"><span class="px-2 bg-white text-text-secondary">Ou continue com</span></div>
                </div>
                <div class="mt-6 grid grid-cols-1 gap-3">
                    <#list social.providers as p>
                        <a id="social-${p.alias}" href="${p.loginUrl}" class="w-full flex items-center justify-center px-4 py-3 border border-slate-200 rounded-lg shadow-sm bg-white text-sm font-medium text-text-primary hover:bg-slate-50 transition-colors">
                            <span class="mr-2">${p.displayName!}</span>
                        </a>
                    </#list>
                </div>
            </div>
        </#if>

        <!-- Rodapé / Cadastro -->
        <div class="mt-8 text-center">
            <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                <p class="text-sm text-text-secondary">
                    ${msg("Novo Usuário?")}
                    <a tabindex="6" href="${url.registrationUrl}" class="font-bold text-brand-DEFAULT hover:text-brand-hover ml-1">
                        ${msg("Registre-se")}
                    </a>
                </p>
            </#if>
        </div>
    </div>

    <!-- Footer Copyright -->
    <div class="p-6 text-center text-xs text-text-secondary border-t border-slate-100">
        &copy; ${.now?string('yyyy')} ${realm.displayName!'Marketplace'}. Todos os direitos reservados.
    </div>
</div>

</body>
</html>