package com.example.hangsha_android.ui.view.timetable

internal fun buildSnuttCompatibilityScript(snuttOrigin: String): String {
    val originLiteral = snuttOrigin.asJavaScriptString()
    return """
        (() => {
          const allowedOrigin = $originLiteral;
          if (window.location.origin !== allowedOrigin ||
              window.location.pathname !== '/timetable-picker') return;

          // SNUTT supports ReactNativeWebView.postMessage, but still gates the
          // confirm action on window.opener being present.
          if (window.opener == null) window.opener = window;

          const applyAndroidCompatibility = () => {
            // Google OAuth rejects embedded user agents. Remove the unsupported
            // action and explain the limitation below the remaining login options.
            document.querySelector('[data-testid="google-login"]')?.remove();

            const kakaoLoginButton = document.querySelector(
              '[data-testid="kakao-login"]'
            );
            const existingPolicyNotice = document.querySelector(
              '[data-hangsha-google-policy-notice]'
            );
            if (kakaoLoginButton && !existingPolicyNotice) {
              const policyNotice = document.createElement('p');
              policyNotice.textContent =
                'Google 정책에 따라 앱 내에서는 Google 로그인을 이용할 수 없습니다. ';
              policyNotice.setAttribute('data-hangsha-google-policy-notice', '');
              policyNotice.setAttribute('role', 'note');
              Object.assign(policyNotice.style, {
                margin: '0',
                color: 'rgba(0, 0, 0, 0.60)',
                fontFamily: 'inherit',
                fontSize: '12px',
                lineHeight: '1.5',
                textAlign: 'center',
                wordBreak: 'keep-all'
              });
              kakaoLoginButton.insertAdjacentElement('afterend', policyNotice);
            }

            const idInput = document.querySelector('[data-testid="id-input"]');
            if (idInput) {
              const form = idInput.closest('form');
              const container = form && form.parentElement;
              const wrapper = container && container.parentElement;
              if (wrapper && container && form) {
                wrapper.style.height = 'auto';
                wrapper.style.minHeight = '100vh';
                wrapper.style.alignItems = 'flex-start';
                wrapper.style.overflowY = 'auto';
                wrapper.style.padding = '16px 0';
                container.style.margin = '0 auto';
                container.style.padding = '24px 20px';
              }
            }

            const pickerWrapper = document.querySelector('#root')?.firstElementChild;
            if (!idInput && pickerWrapper?.children.length === 2 && window.innerWidth <= 700) {
              const leftPane = pickerWrapper.children[0];
              const rightPane = pickerWrapper.children[1];
              if (leftPane && rightPane) {
                const wrapper = pickerWrapper;
                wrapper.style.flexDirection = 'column';
                wrapper.style.height = 'auto';
                wrapper.style.minHeight = '100vh';
                wrapper.style.overflowY = 'auto';
                leftPane.style.width = '100%';
                leftPane.style.height = '38vh';
                leftPane.style.minHeight = '220px';
                leftPane.style.borderRight = 'none';
                leftPane.style.borderBottom = '10px solid rgb(232, 235, 240)';
                leftPane.style.boxShadow = '0 3px 10px rgba(0, 0, 0, 0.10)';
                leftPane.style.boxSizing = 'border-box';
                leftPane.style.position = 'relative';
                leftPane.style.zIndex = '1';
                rightPane.style.width = '100%';
                rightPane.style.minHeight = '62vh';
                rightPane.style.padding = '16px 12px 24px';
                rightPane.style.overflowX = 'auto';
                rightPane.style.position = 'relative';
                rightPane.setAttribute('data-hangsha-picker-preview', '');

                const confirmButton = rightPane.querySelector(
                  '[data-testid="timetable-picker-confirm"]'
                );
                if (confirmButton &&
                    !rightPane.querySelector('[data-hangsha-snutt-logout]')) {
                  const logoutButton = document.createElement('button');
                  logoutButton.type = 'button';
                  logoutButton.textContent = 'SNUTT에서 로그아웃';
                  logoutButton.setAttribute('data-hangsha-snutt-logout', '');
                  logoutButton.setAttribute('aria-label', 'SNUTT에서 로그아웃');
                  Object.assign(logoutButton.style, {
                    position: 'absolute',
                    left: '12px',
                    bottom: '24px',
                    height: '36px',
                    padding: '0 16px',
                    border: '1px solid #9a9a9a',
                    borderRadius: '18px',
                    color: '#666',
                    backgroundColor: 'transparent',
                    cursor: 'pointer',
                    fontFamily: 'inherit',
                    fontSize: '13px',
                    lineHeight: '34px',
                    whiteSpace: 'nowrap'
                  });
                  logoutButton.addEventListener('click', () => {
                    localStorage.removeItem('snutt_token');
                    sessionStorage.removeItem('snutt_token');
                    window.location.reload();
                  });
                  rightPane.appendChild(logoutButton);
                }

                const styleId = 'hangsha-picker-mobile-style';
                if (!document.getElementById(styleId)) {
                  const style = document.createElement('style');
                  style.id = styleId;
                  style.textContent = [
                    '@media (max-width: 700px) {',
                    '  [data-hangsha-picker-preview]::before {',
                    '    content: "선택한 시간표";',
                    '    display: block;',
                    '    flex: 0 0 auto;',
                    '    padding: 2px 4px 0;',
                    '    color: rgba(0, 0, 0, 0.62);',
                    '    font-size: 14px;',
                    '    font-weight: 600;',
                    '    letter-spacing: -0.2px;',
                    '  }',
                    '}'
                  ].join('\n');
                  (document.head || document.documentElement).appendChild(style);
                }
              }
            }
          };

          const start = () => {
            const root = document.documentElement;
            if (!root) return;
            applyAndroidCompatibility();
            new MutationObserver(applyAndroidCompatibility).observe(
              root,
              { childList: true, subtree: true }
            );
          };

          if (document.documentElement) {
            start();
          } else {
            document.addEventListener('DOMContentLoaded', start, { once: true });
          }
        })();
    """.trimIndent()
}

private fun String.asJavaScriptString(): String {
    val slash = 92.toChar().toString()
    return "'" + replace(slash, slash + slash).replace("'", slash + "'") + "'"
}
