/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // "standalone" produces a minimal self-contained build for the Docker image.
  output: "standalone",
};

export default nextConfig;
