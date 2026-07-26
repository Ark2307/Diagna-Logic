export interface SortSpec {
    field: string;
    order: "asc" | "desc";
}

/**
 * Parses this app's existing {@code "field,asc|desc"} sort-string
 * convention into the {@code {field, order}} shape the POST search
 * endpoints expect in their request body — shared by every search
 * function that builds one of those bodies.
 */
export function parseSort(sort?: string): SortSpec | undefined {
    if (!sort) {
        return undefined;
    }
    const [field, order] = sort.split(",");
    if (!field) {
        return undefined;
    }
    return { field, order: order === "desc" ? "desc" : "asc" };
}
