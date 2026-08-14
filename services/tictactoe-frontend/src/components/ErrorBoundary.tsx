import { Component, type ErrorInfo, type ReactNode } from 'react'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false }

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled error rendering the UI:', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="app">
          <p className="error" role="alert">
            Something went wrong. Please reload the page.
          </p>
        </main>
      )
    }
    return this.props.children
  }
}

export default ErrorBoundary
