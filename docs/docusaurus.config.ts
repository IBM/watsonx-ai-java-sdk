import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import { lightTheme, darkTheme } from './src/prism-themes';

const config: Config = {
  title: 'IBM watsonx.ai Java SDK',
  tagline: 'Java SDK for watsonx.ai',
  favicon: 'img/favicon.ico',
  customFields: {
    sdkVersion: '0.30.1',
  },

  future: {
    v4: true,
  },

  url: 'https://ibm.github.io',
  baseUrl: '/watsonx-ai-java-sdk/',

  organizationName: 'IBM',
  projectName: 'watsonx-ai-java-sdk',

  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          path: './content',
          routeBasePath: '/',
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/IBM/watsonx-ai-java-sdk/edit/main/docs/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      defaultMode: 'light',
      respectPrefersColorScheme: true,
    },
    docs: {
      sidebar: {
        hideable: true,
        autoCollapseCategories: true,
      },
    },
    tableOfContents: {
      minHeadingLevel: 2,
      maxHeadingLevel: 3,
    },
    navbar: {
      title: '',
      logo: {
        alt: 'IBM Logo',
        src: 'img/ibm-logo.png',
      },
      items: [
        {
          to: '/',
          label: 'Getting Started',
          position: 'left',
          activeBaseRegex: '^/watsonx-ai-java-sdk/$',
        },
        {
          to: '/setup',
          label: 'Setup & Prerequisites',
          position: 'left',
        },
        {
          to: '/authentication',
          label: 'Authentication',
          position: 'left',
        },
        {
          to: '/services',
          label: 'Services',
          position: 'left',
          activeBasePath: 'services',
        },
        {
          to: '/advanced',
          label: 'Advanced',
          position: 'left',
          activeBasePath: 'advanced',
        },
        {
          to: '/migration',
          label: 'Migration',
          position: 'left',
          activeBasePath: 'migration',
        },
        {
          href: 'https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples',
          label: 'Examples',
          position: 'left',
        },
        {
          href: 'https://javadoc.io/doc/com.ibm.watsonx/watsonx-ai/latest/index.html',
          label: 'Javadoc',
          position: 'left',
        },
        {
          href: 'https://github.com/IBM/watsonx-ai-java-sdk',
          position: 'right',
          className: 'header-github-link',
          'aria-label': 'GitHub repository',
        },
      ],
    },
    footer: {
      style: 'light',
      links: [
        {
          title: 'Documentation',
          items: [
            { label: 'Getting Started', to: '/' },
            { label: 'Setup', to: '/setup' },
            { label: 'Authentication', to: '/authentication' },
            { label: 'Services', to: '/services' },
          ],
        },
        {
          title: 'Resources',
          items: [
            { label: 'GitHub', href: 'https://github.com/IBM/watsonx-ai-java-sdk' },
            { label: 'API Reference', href: 'https://cloud.ibm.com/apidocs/watsonx-ai' },
            { label: 'IBM watsonx.ai', href: 'https://www.ibm.com/watsonx' },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} IBM Corporation. Licensed under the Apache License 2.0.`,
    },
    prism: {
      theme: lightTheme,
      darkTheme: darkTheme,
      additionalLanguages: ['java', 'bash', 'json', 'groovy'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
