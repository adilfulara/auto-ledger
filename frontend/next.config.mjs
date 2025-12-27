/** @type {import('next').NextConfig} */
const nextConfig = {
    rewrites: async () => {
        return [
            {
                source: "/api/:path*",
                destination: "http://localhost:9090/api/:path*", // Proxy to Spring Boot Backend
            },
        ];
    },
};

export default nextConfig;
