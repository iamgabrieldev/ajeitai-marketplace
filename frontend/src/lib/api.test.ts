/**
 * @jest-environment node
 */
import { prestadoresApi, agendamentosApi, ApiError } from "./api";

const originalFetch = globalThis.fetch;

describe("api", () => {
  beforeEach(() => {
    globalThis.fetch = jest.fn();
  });
  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("returns json on 200", async () => {
    (fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ id: 1, nomeFantasia: "Test" }),
    });
    const result = await prestadoresApi.getById("token", "1");
    expect(result).toEqual({ id: 1, nomeFantasia: "Test" });
  });

  it("throws ApiError on 404", async () => {
    (fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 404,
      json: () => Promise.resolve({ mensagem: "Não encontrado" }),
    });
    await expect(prestadoresApi.getById("token", "999")).rejects.toMatchObject({
      name: "ApiError",
      status: 404,
      message: "Não encontrado",
    });
  });

  it("listar returns array on 200", async () => {
    (fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ content: [], totalElements: 0 }),
    });
    const result = await agendamentosApi.listar("token");
    expect(result).toEqual({ content: [], totalElements: 0 });
  });
});
