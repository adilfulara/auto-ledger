import { UserButton } from "@clerk/nextjs";

export default function MainLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <div className="flex min-h-screen flex-col">
            <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
                <div className="container flex h-14 items-center">
                    <div className="mr-4 flex">
                        <a className="mr-6 flex items-center space-x-2" href="/">
                            <span className="hidden font-bold sm:inline-block">Auto Ledger</span>
                        </a>
                    </div>
                    <div className="flex flex-1 items-center justify-end space-x-2">
                        <UserButton />
                    </div>
                </div>
            </header>
            <main className="flex-1 container py-6">{children}</main>
        </div>
    );
}
