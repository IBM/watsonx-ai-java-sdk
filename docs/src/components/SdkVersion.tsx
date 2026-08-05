import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

export default function SdkVersion(): string {
  const { siteConfig } = useDocusaurusContext();
  return (siteConfig.customFields as { sdkVersion: string }).sdkVersion;
}
