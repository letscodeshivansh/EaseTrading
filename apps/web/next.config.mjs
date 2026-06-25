/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // "standalone" is only needed for the Docker image (self-hosting). On Vercel we
  // must NOT set it, or routing can 404. The Docker build sets BUILD_STANDALONE=true.
  output: process.env.BUILD_STANDALONE === "true" ? "standalone" : undefined,
};

export default nextConfig;
