# The proxy config is baked into the image instead of bind-mounted, so the
# stack also deploys when Jenkins itself runs inside a container.
FROM nginx:1.27-alpine@sha256:65645c7bb6a0661892a8b03b89d0743208a18dd2f3f17a54ef4b76fb8e2f2a10

COPY nginx.conf /etc/nginx/nginx.conf
