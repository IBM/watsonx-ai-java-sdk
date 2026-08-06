import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  mainSidebar: [
    {
      type: 'doc',
      id: 'index',
      label: 'Getting Started',
    },
    {
      type: 'doc',
      id: 'setup',
      label: 'Setup & Prerequisites',
    },
    {
      type: 'doc',
      id: 'authentication',
      label: 'Authentication',
    },
    {
      type: 'category',
      label: 'Services',
      link: { type: 'doc', id: 'services/index' },
      collapsed: false,
      items: [
        { type: 'doc', id: 'services/chat-service',            label: 'Chat' },
        { type: 'doc', id: 'services/embedding-service',       label: 'Embedding' },
        { type: 'doc', id: 'services/rerank-service',          label: 'Rerank' },
        { type: 'doc', id: 'services/detection-service',       label: 'Detection' },
        { type: 'doc', id: 'services/tokenization-service',    label: 'Tokenization' },
        { type: 'doc', id: 'services/foundation-model-service', label: 'Foundation Models' },
        {
          type: 'category',
          label: 'Document Processing',
          link: { type: 'doc', id: 'services/document-processing/document-processing' },
          collapsed: true,
          items: [
            {
              type: 'category',
              label: 'Schema',
              link: { type: 'doc', id: 'services/document-processing/schema/index' },
              collapsed: true,
              items: [
                { type: 'doc', id: 'services/document-processing/schema/create-schema-service',   label: 'Create Schema' },
                { type: 'doc', id: 'services/document-processing/schema/improve-schema-service',  label: 'Improve Schema' },
                { type: 'doc', id: 'services/document-processing/schema/merge-schema-service',    label: 'Merge Schema' },
                { type: 'doc', id: 'services/document-processing/schema/cluster-schema-service',  label: 'Cluster Schema' },
              ],
            },
            { type: 'doc', id: 'services/document-processing/text-extraction-service',  label: 'Text Extraction' },
            { type: 'doc', id: 'services/document-processing/text-classification-service', label: 'Text Classification' },
          ],
        },
        { type: 'doc', id: 'services/time-series-service',         label: 'Time Series' },
        { type: 'doc', id: 'services/tool-service',               label: 'Tool Service' },
        { type: 'doc', id: 'services/deployment-service',         label: 'Deployment' },
        {
          type: 'category',
          label: 'Model Gateway',
          link: { type: 'doc', id: 'services/model-gateway/index' },
          collapsed: true,
          items: [
            { type: 'doc', id: 'services/model-gateway/chat',    label: 'Chat' },
            { type: 'doc', id: 'services/model-gateway/catalog', label: 'Catalog' },
          ],
        },
        { type: 'doc', id: 'services/file-service',  label: 'File Service' },
        { type: 'doc', id: 'services/batch-service', label: 'Batch Service' },
      ],
    },
    {
      type: 'category',
      label: 'Advanced',
      link: { type: 'doc', id: 'advanced/index' },
      collapsed: true,
      items: [
        { type: 'doc', id: 'advanced/error-handling',        label: 'Error Handling' },
        { type: 'doc', id: 'advanced/environment-variables', label: 'Environment Variables' },
        { type: 'doc', id: 'advanced/http-client',           label: 'HTTP Client' },
        { type: 'doc', id: 'advanced/spi',                   label: 'Service Provider Interface' },
      ],
    },
    {
      type: 'category',
      label: 'Migration Guide',
      link: { type: 'doc', id: 'migration/migration' },
      collapsed: true,
      items: [
        { type: 'doc', id: 'migration/0.22.0-to-0.30.0', label: '0.22.0 → 0.30.0' },
      ],
    },
  ],
};

export default sidebars;
