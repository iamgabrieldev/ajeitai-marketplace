import { cn, formatEndereco } from "./utils";

describe("cn", () => {
  it("merges class names", () => {
    expect(cn("a", "b")).toBe("a b");
  });

  it("handles conditional classes", () => {
    expect(cn("base", false && "hidden", "visible")).toContain("base");
  });
});

describe("formatEndereco", () => {
  it("returns empty for null/undefined", () => {
    expect(formatEndereco(null)).toBe("");
    expect(formatEndereco(undefined)).toBe("");
  });

  it("returns string as-is", () => {
    expect(formatEndereco("Rua X, 1")).toBe("Rua X, 1");
  });

  it("formats object address", () => {
    expect(
      formatEndereco({
        logradouro: "Rua A",
        numero: "10",
        bairro: "Centro",
        cidade: "São Paulo",
        uf: "SP",
        cep: "01000-000",
      })
    ).toBe("Rua A, 10, Centro, São Paulo - SP, 01000-000");
  });
});
