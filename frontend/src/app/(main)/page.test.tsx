import { render, screen } from "@testing-library/react";
import Page from "./page";

describe("Home Page", () => {
    it("renders successfully", () => {
        render(<Page />);
        expect(screen.getByText("Auto-Ledger Frontend")).toBeInTheDocument();
    });
});
