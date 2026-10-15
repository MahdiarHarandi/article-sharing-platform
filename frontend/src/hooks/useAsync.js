import { useCallback, useEffect, useRef, useState } from "react";

export function useAsync(asyncFunction, dependencies = [], options = {}) {
  const { immediate = true } = options;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(Boolean(immediate));
  const [error, setError] = useState(null);
  const abortControllerRef = useRef(null);

  const execute = useCallback(async () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const controller = new AbortController();
    abortControllerRef.current = controller;

    setLoading(true);
    setError(null);

    try {
      const result = await asyncFunction(controller.signal);
      setData(result);
      return result;
    } catch (caughtError) {
      if (caughtError.name !== "AbortError") {
        setError(caughtError);
      }
      return null;
    } finally {
      if (!controller.signal.aborted) {
        setLoading(false);
      }
    }
  }, dependencies);

  useEffect(() => {
    if (immediate) {
      execute();
    }

    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, [execute, immediate]);

  return { data, loading, error, execute, setData };
}
