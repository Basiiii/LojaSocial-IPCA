const validateAuth = (req) => {
  const authHeader = req.headers.authorization;
  const expectedPassword = process.env.API_PASSWORD;
  return authHeader === `Bearer ${expectedPassword}`;
};

export { validateAuth };
